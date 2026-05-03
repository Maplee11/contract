package com.bytedance.contract.model;

public enum ContractType {
    RECEIVABLE("应收款"),
    PAYABLE("应付款");

    private final String label;

    ContractType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
