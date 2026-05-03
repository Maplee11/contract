package com.bytedance.contract.dto;

import com.bytedance.contract.model.ContractType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReminderView(
        UUID contractId,
        String cycleKey,
        ContractType type,
        String typeLabel,
        String signatoryCompany,
        String projectName,
        String counterpartyCompany,
        BigDecimal cycleAmount,
        LocalDate cycleStartDate,
        LocalDate cycleEndDate,
        LocalDate dueDate,
        LocalDate reminderDate,
        long daysUntilDue,
        double progressPercent
) {
}
