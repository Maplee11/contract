package com.bytedance.contract.dto;

import java.math.BigDecimal;

public record ProfitStatView(
        String signatoryCompany,
        String projectName,
        BigDecimal cumulativeReceivableDueAmount,
        BigDecimal cumulativePayableDueAmount,
        BigDecimal cumulativeReceivedAmount,
        BigDecimal cumulativePaidAmount,
        BigDecimal expectedProfitAmount,
        BigDecimal actualProfitAmount
) {
}
