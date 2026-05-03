package com.bytedance.contract.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytedance.contract.dto.DashboardResponse;
import com.bytedance.contract.dto.SettleCycleRequest;
import com.bytedance.contract.model.Contract;
import com.bytedance.contract.model.ContractType;
import com.bytedance.contract.storage.ContractStorageService;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContractServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void calculateMetricsShouldExcludeRentFreePeriodAndRoundUpCycles() {
        ContractService service = new ContractService(new ContractStorageService(tempDir.resolve("contracts.json").toString()));

        Contract contract = buildContract(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 6, 30),
                2
        );

        ContractService.ContractMetrics metrics = service.calculateMetrics(contract);

        assertThat(metrics.cycleAmount()).isEqualByComparingTo("2200.00");
        assertThat(metrics.cycles()).hasSize(3);
        assertThat(metrics.totalAmount()).isEqualByComparingTo("6600.00");
        assertThat(metrics.chargeableMonths()).isEqualTo(5);
        assertThat(metrics.cycles().get(0).startDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(metrics.cycles().get(2).endDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void settleCycleShouldRecordActualAmountAndAdvanceToNextCycle() {
        ContractStorageService storageService = new ContractStorageService(tempDir.resolve("contracts.json").toString());
        ContractService service = new ContractService(storageService);

        LocalDate dueDate = LocalDate.now().minusDays(5);
        Contract contract = buildContract(
                dueDate,
                null,
                dueDate.plusMonths(2).plusDays(5),
                1
        );
        storageService.saveAll(List.of(contract));

        DashboardResponse initialDashboard = service.getDashboard();
        assertThat(initialDashboard.reminders()).hasSize(1);
        assertThat(initialDashboard.contractStats()).hasSize(1);
        assertThat(initialDashboard.contractStats().get(0).currentCycleKey()).isEqualTo(initialDashboard.reminders().get(0).cycleKey());

        String cycleKey = initialDashboard.reminders().get(0).cycleKey();
        SettleCycleRequest settleCycleRequest = new SettleCycleRequest();
        settleCycleRequest.setFullPayment(false);
        settleCycleRequest.setActualAmount(new BigDecimal("888.88"));
        service.settleCycle(contract.getId(), cycleKey, settleCycleRequest);

        DashboardResponse updatedDashboard = service.getDashboard();
        assertThat(updatedDashboard.contracts().get(0).cumulativeActualAmount()).isEqualByComparingTo("888.88");
        assertThat(updatedDashboard.contracts().get(0).settledCycles()).isEqualTo(1);
        assertThat(updatedDashboard.contractStats().get(0).currentCycleKey()).isNotEqualTo(cycleKey);
    }

    @Test
    void dashboardShouldExposeInputSuggestions() {
        ContractStorageService storageService = new ContractStorageService(tempDir.resolve("contracts.json").toString());
        ContractService service = new ContractService(storageService);

        Contract first = buildContract(LocalDate.now(), null, LocalDate.now().plusMonths(3), 1);
        first.setProjectName("园区 A");
        Contract second = buildContract(LocalDate.now().plusMonths(1), null, LocalDate.now().plusMonths(6), 1);
        second.setProjectName("园区 B");
        second.setSignatoryCompany("集团总部");
        storageService.saveAll(List.of(first, second));

        DashboardResponse dashboard = service.getDashboard();
        assertThat(dashboard.inputSuggestions().projectNames()).containsExactly("园区 B", "园区 A");
        assertThat(dashboard.inputSuggestions().signatoryCompanies()).contains("集团总部", "甲方公司");
        assertThat(dashboard.profitStats()).hasSize(2);
    }

    @Test
    void cumulativeDueAmountShouldOnlyIncludeCyclesInReminderWindow() {
        ContractStorageService storageService = new ContractStorageService(tempDir.resolve("contracts.json").toString());
        ContractService service = new ContractService(storageService);

        LocalDate leaseStartDate = LocalDate.now().minusDays(10);
        Contract contract = buildContract(
                leaseStartDate,
                null,
                leaseStartDate.plusMonths(3).plusDays(5),
                1
        );
        storageService.saveAll(List.of(contract));

        DashboardResponse dashboard = service.getDashboard();
        assertThat(dashboard.contractStats()).hasSize(1);
        assertThat(dashboard.contractStats().get(0).cumulativeDueAmount()).isEqualByComparingTo("1100.00");
        assertThat(dashboard.contractStats().get(0).overdueAmount()).isEqualByComparingTo("1100.00");
    }

    @Test
    void deleteContractShouldSoftDeleteInsteadOfRemovingFromJson() throws Exception {
        Path storagePath = tempDir.resolve("contracts.json");
        ContractStorageService storageService = new ContractStorageService(storagePath.toString());
        ContractService service = new ContractService(storageService);

        Contract contract = buildContract(LocalDate.now(), null, LocalDate.now().plusMonths(1), 1);
        storageService.saveAll(List.of(contract));

        service.deleteContract(contract.getId());

        List<Contract> storedContracts = storageService.loadAll();
        assertThat(storedContracts).hasSize(1);
        assertThat(storedContracts.get(0).isDeleted()).isTrue();
        assertThat(service.getDashboard().contracts()).isEmpty();
        assertThat(Files.readString(storagePath)).contains("\"deleted\" : true");
    }

    @Test
    void expiredContractShouldBeArchivedAndHiddenFromDashboard() {
        ContractStorageService storageService = new ContractStorageService(tempDir.resolve("contracts.json").toString());
        ContractService service = new ContractService(storageService);

        Contract expiredContract = buildContract(
                LocalDate.now().minusMonths(3),
                null,
                LocalDate.now().minusDays(1),
                1
        );
        storageService.saveAll(List.of(expiredContract));

        DashboardResponse dashboard = service.getDashboard();
        List<Contract> storedContracts = storageService.loadAll();

        assertThat(dashboard.contracts()).isEmpty();
        assertThat(dashboard.reminders()).isEmpty();
        assertThat(dashboard.contractStats()).isEmpty();
        assertThat(storedContracts).hasSize(1);
        assertThat(storedContracts.get(0).isDeleted()).isTrue();
    }

    private Contract buildContract(
            LocalDate leaseStartDate,
            LocalDate rentFreeEndDate,
            LocalDate contractEndDate,
            int paymentCycleMonths
    ) {
        Contract contract = new Contract();
        contract.setId(UUID.randomUUID());
        contract.setType(ContractType.RECEIVABLE);
        contract.setSignatoryCompany("甲方公司");
        contract.setCounterpartyCompany("乙方公司");
        contract.setProjectName("测试项目");
        contract.setSigningDate(leaseStartDate.minusDays(10));
        contract.setSignedArea("1200 平方米");
        contract.setLeaseStartDate(leaseStartDate);
        contract.setRentFreeEndDate(rentFreeEndDate);
        contract.setContractEndDate(contractEndDate);
        contract.setMonthlyRent(new BigDecimal("1000"));
        contract.setMonthlyPropertyFee(new BigDecimal("100"));
        contract.setPaymentCycleMonths(paymentCycleMonths);
        contract.setCreatedAt(LocalDateTime.now());
        return contract;
    }
}
