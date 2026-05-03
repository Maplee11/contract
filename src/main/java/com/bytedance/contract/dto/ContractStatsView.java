package com.bytedance.contract.dto;

import com.bytedance.contract.model.ContractType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractStatsView(
        UUID contractId,
        ContractType type,
        String typeLabel,
        String signatoryCompany,
        String counterpartyCompany,
        String projectName,
        String currentCycleKey,
        LocalDate cycleStartDate,
        LocalDate cycleEndDate,
        LocalDate dueDate,
        BigDecimal currentCycleAmount,
        BigDecimal cumulativeDueAmount,
        BigDecimal cumulativeActualAmount,
        double progressPercent,
        long daysUntilDue,
        boolean settled
) {
}
