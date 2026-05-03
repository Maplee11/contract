package com.bytedance.contract.dto;

import com.bytedance.contract.model.ContractType;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ContractRequest {

    private ContractType type;
    private String signatoryCompany;
    private String counterpartyCompany;
    private String projectName;
    private LocalDate signingDate;
    private String signedArea;
    private LocalDate leaseStartDate;
    private LocalDate rentFreeEndDate;
    private LocalDate contractEndDate;
    private BigDecimal monthlyRent;
    private BigDecimal monthlyPropertyFee;
    private Integer paymentCycleMonths;

    public ContractType getType() {
        return type;
    }

    public void setType(ContractType type) {
        this.type = type;
    }

    public String getSignatoryCompany() {
        return signatoryCompany;
    }

    public void setSignatoryCompany(String signatoryCompany) {
        this.signatoryCompany = signatoryCompany;
    }

    public String getCounterpartyCompany() {
        return counterpartyCompany;
    }

    public void setCounterpartyCompany(String counterpartyCompany) {
        this.counterpartyCompany = counterpartyCompany;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public LocalDate getSigningDate() {
        return signingDate;
    }

    public void setSigningDate(LocalDate signingDate) {
        this.signingDate = signingDate;
    }

    public String getSignedArea() {
        return signedArea;
    }

    public void setSignedArea(String signedArea) {
        this.signedArea = signedArea;
    }

    public LocalDate getLeaseStartDate() {
        return leaseStartDate;
    }

    public void setLeaseStartDate(LocalDate leaseStartDate) {
        this.leaseStartDate = leaseStartDate;
    }

    public LocalDate getRentFreeEndDate() {
        return rentFreeEndDate;
    }

    public void setRentFreeEndDate(LocalDate rentFreeEndDate) {
        this.rentFreeEndDate = rentFreeEndDate;
    }

    public LocalDate getContractEndDate() {
        return contractEndDate;
    }

    public void setContractEndDate(LocalDate contractEndDate) {
        this.contractEndDate = contractEndDate;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public BigDecimal getMonthlyPropertyFee() {
        return monthlyPropertyFee;
    }

    public void setMonthlyPropertyFee(BigDecimal monthlyPropertyFee) {
        this.monthlyPropertyFee = monthlyPropertyFee;
    }

    public Integer getPaymentCycleMonths() {
        return paymentCycleMonths;
    }

    public void setPaymentCycleMonths(Integer paymentCycleMonths) {
        this.paymentCycleMonths = paymentCycleMonths;
    }
}
