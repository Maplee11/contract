package com.bytedance.contract.dto;

import java.math.BigDecimal;

public class SettleCycleRequest {

    private BigDecimal actualAmount;
    private Boolean fullPayment;

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public Boolean getFullPayment() {
        return fullPayment;
    }

    public void setFullPayment(Boolean fullPayment) {
        this.fullPayment = fullPayment;
    }
}
