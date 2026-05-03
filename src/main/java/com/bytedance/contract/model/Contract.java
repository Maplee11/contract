package com.bytedance.contract.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Contract {

    private UUID id;
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
    private Map<String, BigDecimal> actualCycleAmounts = new LinkedHashMap<>();
    private Set<String> completedCycleKeys = new LinkedHashSet<>();
    private Set<String> arrearsCycleKeys = new LinkedHashSet<>();
    private LocalDateTime createdAt;
    private boolean deleted;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public Map<String, BigDecimal> getActualCycleAmounts() {
        return actualCycleAmounts;
    }

    public void setActualCycleAmounts(Map<String, BigDecimal> actualCycleAmounts) {
        this.actualCycleAmounts = actualCycleAmounts == null ? new LinkedHashMap<>() : actualCycleAmounts;
    }

    public Set<String> getCompletedCycleKeys() {
        return completedCycleKeys;
    }

    public void setCompletedCycleKeys(Set<String> completedCycleKeys) {
        this.completedCycleKeys = completedCycleKeys == null ? new LinkedHashSet<>() : completedCycleKeys;
    }

    public Set<String> getArrearsCycleKeys() {
        return arrearsCycleKeys;
    }

    public void setArrearsCycleKeys(Set<String> arrearsCycleKeys) {
        this.arrearsCycleKeys = arrearsCycleKeys == null ? new LinkedHashSet<>() : arrearsCycleKeys;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
