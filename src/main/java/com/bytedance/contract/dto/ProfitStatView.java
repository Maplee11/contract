package com.bytedance.contract.dto;

import java.math.BigDecimal;

public record ProfitStatView(
        String signatoryCompany,
        String projectName,
        BigDecimal cumulativeDueAmount,
        BigDecimal cumulativeActualAmount,
        BigDecimal gapAmount
) {
}
