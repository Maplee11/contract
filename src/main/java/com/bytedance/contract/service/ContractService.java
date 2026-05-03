package com.bytedance.contract.service;

import com.bytedance.contract.dto.ContractRequest;
import com.bytedance.contract.dto.ContractStatsView;
import com.bytedance.contract.dto.ContractView;
import com.bytedance.contract.dto.DashboardResponse;
import com.bytedance.contract.dto.InputSuggestions;
import com.bytedance.contract.dto.ProfitStatView;
import com.bytedance.contract.dto.ReminderView;
import com.bytedance.contract.dto.SettleCycleRequest;
import com.bytedance.contract.model.Contract;
import com.bytedance.contract.model.ContractType;
import com.bytedance.contract.storage.ContractStorageService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ContractService {

    private static final long REMINDER_ADVANCE_DAYS = 15L;

    private final ContractStorageService storageService;

    public ContractService(ContractStorageService storageService) {
        this.storageService = storageService;
    }

    public DashboardResponse getDashboard() {
        List<Contract> contracts = storageService.loadAll();
        archiveExpiredContracts(contracts, LocalDate.now());
        List<Contract> activeContracts = contracts.stream()
                .filter(contract -> !contract.isDeleted())
                .toList();
        List<ContractView> contractViews = new ArrayList<>();
        List<ReminderView> reminders = new ArrayList<>();
        List<ContractStatsView> contractStats = new ArrayList<>();
        List<ProfitStatView> profitStats = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Map<String, ProfitAccumulator> profitMap = new LinkedHashMap<>();

        for (Contract contract : activeContracts) {
            ContractMetrics metrics = calculateMetrics(contract);
            Map<String, BigDecimal> settledAmounts = resolveSettledAmounts(contract, metrics);
            BillingCycle currentCycle = findCurrentCycle(metrics.cycles(), settledAmounts.keySet());
            BigDecimal cumulativeDueAmount = calculateCumulativeDueAmount(metrics, today);
            BigDecimal cumulativeActualAmount = sumAmounts(settledAmounts);

            contractViews.add(toContractView(contract, metrics, settledAmounts));
            contractStats.add(toContractStatsView(
                    contract,
                    metrics,
                    settledAmounts,
                    currentCycle,
                    cumulativeDueAmount,
                    cumulativeActualAmount,
                    today
            ));

            if (currentCycle != null && !today.isBefore(currentCycle.reminderDate())) {
                reminders.add(new ReminderView(
                        contract.getId(),
                        currentCycle.key(),
                        contract.getType(),
                        contract.getType().getLabel(),
                        contract.getSignatoryCompany(),
                        contract.getProjectName(),
                        contract.getCounterpartyCompany(),
                        metrics.cycleAmount(),
                        currentCycle.startDate(),
                        currentCycle.endDate(),
                        currentCycle.dueDate(),
                        currentCycle.reminderDate(),
                        ChronoUnit.DAYS.between(today, currentCycle.dueDate()),
                        calculateProgressPercent(currentCycle.reminderDate(), currentCycle.dueDate(), today)
                ));
            }

            if (contract.getType() == ContractType.RECEIVABLE) {
                String profitKey = contract.getSignatoryCompany() + "||" + contract.getProjectName();
                ProfitAccumulator accumulator = profitMap.computeIfAbsent(
                        profitKey,
                        ignored -> new ProfitAccumulator(contract.getSignatoryCompany(), contract.getProjectName())
                );
                accumulator.addDue(cumulativeDueAmount);
                accumulator.addActual(cumulativeActualAmount);
            }
        }

        contractViews.sort(Comparator.comparing(ContractView::createdAt).reversed());
        reminders.sort(Comparator.comparing(ReminderView::type).thenComparing(ReminderView::dueDate));
        contractStats.sort(Comparator.comparing(ContractStatsView::projectName));
        profitStats.addAll(profitMap.values().stream()
                .map(ProfitAccumulator::toView)
                .sorted(Comparator.comparing(ProfitStatView::signatoryCompany).thenComparing(ProfitStatView::projectName))
                .toList());

        return new DashboardResponse(contractViews, reminders, contractStats, profitStats, buildSuggestions(activeContracts));
    }

    public Contract createContract(ContractRequest request) {
        validate(request);
        List<Contract> contracts = storageService.loadAll();

        Contract contract = new Contract();
        contract.setId(UUID.randomUUID());
        contract.setCreatedAt(LocalDateTime.now());
        applyRequest(contract, request);

        contracts.add(contract);
        storageService.saveAll(contracts);
        return contract;
    }

    public Contract updateContract(UUID contractId, ContractRequest request) {
        validate(request);
        List<Contract> contracts = storageService.loadAll();
        Contract contract = findActiveContract(contracts, contractId);

        applyRequest(contract, request);
        pruneCycleState(contract);
        storageService.saveAll(contracts);
        return contract;
    }

    public void deleteContract(UUID contractId) {
        List<Contract> contracts = storageService.loadAll();
        Contract contract = findActiveContract(contracts, contractId);
        contract.setDeleted(true);
        storageService.saveAll(contracts);
    }

    public void settleCycle(UUID contractId, String cycleKey, SettleCycleRequest request) {
        List<Contract> contracts = storageService.loadAll();
        Contract contract = findActiveContract(contracts, contractId);
        ContractMetrics metrics = calculateMetrics(contract);
        BillingCycle cycle = findCycle(metrics.cycles(), cycleKey);

        BigDecimal actualAmount = resolveActualAmount(metrics.cycleAmount(), request);
        contract.getActualCycleAmounts().put(cycle.key(), actualAmount);
        contract.getCompletedCycleKeys().add(cycle.key());
        contract.getArrearsCycleKeys().remove(cycle.key());
        storageService.saveAll(contracts);
    }

    private void applyRequest(Contract contract, ContractRequest request) {
        contract.setType(request.getType());
        contract.setSignatoryCompany(request.getSignatoryCompany().trim());
        contract.setCounterpartyCompany(request.getCounterpartyCompany().trim());
        contract.setProjectName(request.getProjectName().trim());
        contract.setSigningDate(request.getSigningDate());
        contract.setSignedArea(request.getSignedArea().trim());
        contract.setLeaseStartDate(request.getLeaseStartDate());
        contract.setRentFreeEndDate(request.getRentFreeEndDate());
        contract.setContractEndDate(request.getContractEndDate());
        contract.setMonthlyRent(scaleAmount(request.getMonthlyRent()));
        contract.setMonthlyPropertyFee(scaleAmount(request.getMonthlyPropertyFee()));
        contract.setPaymentCycleMonths(request.getPaymentCycleMonths());
    }

    private void validate(ContractRequest request) {
        if (request.getType() == null) {
            throw new IllegalArgumentException("请选择合同类型");
        }
        if (isBlank(request.getSignatoryCompany()) || isBlank(request.getCounterpartyCompany()) || isBlank(request.getProjectName())
                || isBlank(request.getSignedArea())) {
            throw new IllegalArgumentException("请完整填写合同基础信息");
        }
        if (request.getSigningDate() == null || request.getLeaseStartDate() == null || request.getContractEndDate() == null) {
            throw new IllegalArgumentException("请完整填写合同日期");
        }
        if (Objects.isNull(request.getMonthlyRent()) || Objects.isNull(request.getMonthlyPropertyFee())
                || Objects.isNull(request.getPaymentCycleMonths())) {
            throw new IllegalArgumentException("请完整填写费用与周期");
        }
        if (request.getMonthlyRent().compareTo(BigDecimal.ZERO) < 0 || request.getMonthlyPropertyFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负数");
        }
        if (request.getPaymentCycleMonths() <= 0) {
            throw new IllegalArgumentException("缴费周期必须大于 0");
        }
        if (!request.getContractEndDate().isAfter(request.getLeaseStartDate())) {
            throw new IllegalArgumentException("合同终止时间必须晚于起租时间");
        }
        if (request.getRentFreeEndDate() != null && request.getRentFreeEndDate().isAfter(request.getContractEndDate())) {
            throw new IllegalArgumentException("免租截止日不能晚于合同终止时间");
        }
    }

    private Contract findActiveContract(List<Contract> contracts, UUID contractId) {
        return contracts.stream()
                .filter(item -> item.getId().equals(contractId) && !item.isDeleted())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到合同"));
    }

    private void archiveExpiredContracts(List<Contract> contracts, LocalDate today) {
        boolean changed = false;
        for (Contract contract : contracts) {
            if (!contract.isDeleted() && contract.getContractEndDate() != null && contract.getContractEndDate().isBefore(today)) {
                contract.setDeleted(true);
                changed = true;
            }
        }
        if (changed) {
            storageService.saveAll(contracts);
        }
    }

    private BillingCycle findCycle(List<BillingCycle> cycles, String cycleKey) {
        return cycles.stream()
                .filter(cycle -> cycle.key().equals(cycleKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到对应缴费周期"));
    }

    private BigDecimal resolveActualAmount(BigDecimal cycleAmount, SettleCycleRequest request) {
        if (request == null || Boolean.TRUE.equals(request.getFullPayment())) {
            return cycleAmount;
        }
        if (request.getActualAmount() == null) {
            throw new IllegalArgumentException("请输入本期实收实付款");
        }
        if (request.getActualAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("本期实收实付款不能为负数");
        }
        return scaleAmount(request.getActualAmount());
    }

    private void pruneCycleState(Contract contract) {
        Set<String> validKeys = new LinkedHashSet<>();
        for (BillingCycle cycle : calculateMetrics(contract).cycles()) {
            validKeys.add(cycle.key());
        }
        contract.getActualCycleAmounts().entrySet().removeIf(entry -> !validKeys.contains(entry.getKey()));
        contract.getCompletedCycleKeys().removeIf(key -> !validKeys.contains(key));
        contract.getArrearsCycleKeys().removeIf(key -> !validKeys.contains(key));
    }

    private ContractView toContractView(Contract contract, ContractMetrics metrics, Map<String, BigDecimal> settledAmounts) {
        return new ContractView(
                contract.getId(),
                contract.getType(),
                contract.getType().getLabel(),
                contract.getSignatoryCompany(),
                contract.getCounterpartyCompany(),
                contract.getProjectName(),
                contract.getSigningDate(),
                contract.getSignedArea(),
                contract.getLeaseStartDate(),
                contract.getRentFreeEndDate(),
                contract.getContractEndDate(),
                contract.getMonthlyRent(),
                contract.getMonthlyPropertyFee(),
                contract.getPaymentCycleMonths(),
                metrics.cycleAmount(),
                metrics.totalAmount(),
                metrics.cycles().size(),
                metrics.chargeableMonths(),
                sumAmounts(settledAmounts),
                settledAmounts.size(),
                new LinkedHashMap<>(settledAmounts),
                contract.getCreatedAt()
        );
    }

    private ContractStatsView toContractStatsView(
            Contract contract,
            ContractMetrics metrics,
            Map<String, BigDecimal> settledAmounts,
            BillingCycle currentCycle,
            BigDecimal cumulativeDueAmount,
            BigDecimal cumulativeActualAmount,
            LocalDate today
    ) {
        BigDecimal overdueAmount = cumulativeDueAmount.subtract(cumulativeActualAmount).setScale(2, RoundingMode.HALF_UP);
        if (currentCycle == null) {
            return new ContractStatsView(
                    contract.getId(),
                    contract.getType(),
                    contract.getType().getLabel(),
                    contract.getSignatoryCompany(),
                    contract.getCounterpartyCompany(),
                    contract.getProjectName(),
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    cumulativeDueAmount,
                    cumulativeActualAmount,
                    overdueAmount,
                    100D,
                    0L,
                    true
            );
        }

        return new ContractStatsView(
                contract.getId(),
                contract.getType(),
                contract.getType().getLabel(),
                contract.getSignatoryCompany(),
                contract.getCounterpartyCompany(),
                contract.getProjectName(),
                currentCycle.key(),
                currentCycle.startDate(),
                currentCycle.endDate(),
                currentCycle.dueDate(),
                metrics.cycleAmount(),
                cumulativeDueAmount,
                cumulativeActualAmount,
                overdueAmount,
                calculateProgressPercent(currentCycle.reminderDate(), currentCycle.dueDate(), today),
                ChronoUnit.DAYS.between(today, currentCycle.dueDate()),
                false
        );
    }

    private InputSuggestions buildSuggestions(List<Contract> contracts) {
        LinkedHashSet<String> signatoryCompanies = new LinkedHashSet<>();
        LinkedHashSet<String> counterpartyCompanies = new LinkedHashSet<>();
        LinkedHashSet<String> projectNames = new LinkedHashSet<>();
        LinkedHashSet<String> signedAreas = new LinkedHashSet<>();

        contracts.stream()
                .sorted(Comparator.comparing(Contract::getCreatedAt).reversed())
                .forEach(contract -> {
                    signatoryCompanies.add(contract.getSignatoryCompany());
                    counterpartyCompanies.add(contract.getCounterpartyCompany());
                    projectNames.add(contract.getProjectName());
                    signedAreas.add(contract.getSignedArea());
                });

        return new InputSuggestions(
                new ArrayList<>(signatoryCompanies),
                new ArrayList<>(counterpartyCompanies),
                new ArrayList<>(projectNames),
                new ArrayList<>(signedAreas)
        );
    }

    private Map<String, BigDecimal> resolveSettledAmounts(Contract contract, ContractMetrics metrics) {
        Map<String, BigDecimal> settledAmounts = new LinkedHashMap<>();
        Set<String> validCycleKeys = new LinkedHashSet<>();
        for (BillingCycle cycle : metrics.cycles()) {
            validCycleKeys.add(cycle.key());
        }

        if (contract.getActualCycleAmounts() != null) {
            for (Map.Entry<String, BigDecimal> entry : contract.getActualCycleAmounts().entrySet()) {
                if (validCycleKeys.contains(entry.getKey()) && entry.getValue() != null) {
                    settledAmounts.put(entry.getKey(), scaleAmount(entry.getValue()));
                }
            }
        }

        if (contract.getCompletedCycleKeys() != null) {
            for (String cycleKey : contract.getCompletedCycleKeys()) {
                if (validCycleKeys.contains(cycleKey) && !settledAmounts.containsKey(cycleKey)) {
                    settledAmounts.put(cycleKey, metrics.cycleAmount());
                }
            }
        }

        return settledAmounts;
    }

    private BillingCycle findCurrentCycle(List<BillingCycle> cycles, Set<String> settledCycleKeys) {
        for (BillingCycle cycle : cycles) {
            if (!settledCycleKeys.contains(cycle.key())) {
                return cycle;
            }
        }
        return null;
    }

    ContractMetrics calculateMetrics(Contract contract) {
        BigDecimal cycleAmount = contract.getMonthlyRent()
                .add(contract.getMonthlyPropertyFee())
                .multiply(BigDecimal.valueOf(contract.getPaymentCycleMonths()))
                .setScale(2, RoundingMode.HALF_UP);

        LocalDate chargeStartDate = resolveChargeStartDate(contract);
        LocalDate contractEndExclusive = contract.getContractEndDate().plusDays(1);
        List<BillingCycle> cycles = new ArrayList<>();

        if (chargeStartDate.isBefore(contractEndExclusive)) {
            int index = 1;
            for (LocalDate cycleStart = chargeStartDate; cycleStart.isBefore(contractEndExclusive);
                 cycleStart = cycleStart.plusMonths(contract.getPaymentCycleMonths()), index++) {
                LocalDate cycleEndExclusive = cycleStart.plusMonths(contract.getPaymentCycleMonths());
                if (cycleEndExclusive.isAfter(contractEndExclusive)) {
                    cycleEndExclusive = contractEndExclusive;
                }
                cycles.add(new BillingCycle(
                        index,
                        cycleStart.toString(),
                        cycleStart,
                        cycleEndExclusive.minusDays(1),
                        cycleStart,
                        cycleStart.minusDays(REMINDER_ADVANCE_DAYS)
                ));
            }
        }

        BigDecimal totalAmount = cycleAmount
                .multiply(BigDecimal.valueOf(cycles.size()))
                .setScale(2, RoundingMode.HALF_UP);

        return new ContractMetrics(cycleAmount, totalAmount, estimateChargeableMonths(chargeStartDate, contractEndExclusive), cycles);
    }

    private LocalDate resolveChargeStartDate(Contract contract) {
        LocalDate leaseStartDate = contract.getLeaseStartDate();
        if (contract.getRentFreeEndDate() == null || contract.getRentFreeEndDate().isBefore(leaseStartDate)) {
            return leaseStartDate;
        }
        return contract.getRentFreeEndDate().plusDays(1);
    }

    private long estimateChargeableMonths(LocalDate chargeStartDate, LocalDate endExclusive) {
        if (!chargeStartDate.isBefore(endExclusive)) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(chargeStartDate, endExclusive);
        return Math.max(1, (long) Math.ceil(days / 30.0));
    }

    private double calculateProgressPercent(LocalDate reminderDate, LocalDate dueDate, LocalDate today) {
        if (today.isBefore(reminderDate)) {
            return 100D;
        }
        if (!today.isBefore(dueDate)) {
            return 0D;
        }
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(reminderDate, dueDate));
        long remainingDays = Math.max(0, ChronoUnit.DAYS.between(today, dueDate));
        return Math.min(100D, (remainingDays * 100D) / totalDays);
    }

    private BigDecimal calculateCumulativeDueAmount(ContractMetrics metrics, LocalDate today) {
        BigDecimal total = BigDecimal.ZERO;
        for (BillingCycle cycle : metrics.cycles()) {
            if (!today.isBefore(cycle.reminderDate())) {
                total = total.add(metrics.cycleAmount());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumAmounts(Map<String, BigDecimal> amounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal amount : amounts.values()) {
            total = total.add(amount);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record BillingCycle(
            int index,
            String key,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate dueDate,
            LocalDate reminderDate
    ) {
    }

    record ContractMetrics(
            BigDecimal cycleAmount,
            BigDecimal totalAmount,
            long chargeableMonths,
            List<BillingCycle> cycles
    ) {
    }

    private static final class ProfitAccumulator {
        private final String signatoryCompany;
        private final String projectName;
        private BigDecimal dueAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal actualAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        private ProfitAccumulator(String signatoryCompany, String projectName) {
            this.signatoryCompany = signatoryCompany;
            this.projectName = projectName;
        }

        private void addDue(BigDecimal amount) {
            dueAmount = dueAmount.add(amount).setScale(2, RoundingMode.HALF_UP);
        }

        private void addActual(BigDecimal amount) {
            actualAmount = actualAmount.add(amount).setScale(2, RoundingMode.HALF_UP);
        }

        private ProfitStatView toView() {
            return new ProfitStatView(
                    signatoryCompany,
                    projectName,
                    dueAmount,
                    actualAmount,
                    dueAmount.subtract(actualAmount).setScale(2, RoundingMode.HALF_UP)
            );
        }
    }
}
