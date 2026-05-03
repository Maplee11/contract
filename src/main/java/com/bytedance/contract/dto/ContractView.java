package com.bytedance.contract.dto;

import com.bytedance.contract.model.ContractType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ContractView(
        UUID id,
        ContractType type,
        String typeLabel,
        String signatoryCompany,
        String counterpartyCompany,
        String projectName,
        LocalDate signingDate,
        String signedArea,
        LocalDate leaseStartDate,
        LocalDate rentFreeEndDate,
        LocalDate contractEndDate,
        BigDecimal monthlyRent,
        BigDecimal monthlyPropertyFee,
        Integer paymentCycleMonths,
        BigDecimal cycleAmount,
        BigDecimal totalAmount,
        long totalCycles,
        long chargeableMonths,
        BigDecimal cumulativeActualAmount,
        long settledCycles,
        Map<String, BigDecimal> actualCycleAmounts,
        LocalDateTime createdAt
) {
}
