package com.Propertmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Lease {

    private int id;
    private int tenantId;
    private int unitId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private String status;

    public Lease(int tenantId, int unitId, LocalDate startDate, LocalDate endDate,
                 BigDecimal monthlyRent, String status) {
        this.tenantId = tenantId;
        this.unitId = unitId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyRent = monthlyRent;
        this.status = status;
    }

    public Lease(int id, int tenantId, int unitId, LocalDate startDate, LocalDate endDate,
                 BigDecimal monthlyRent, String status) {
        this.id = id;
        this.tenantId = tenantId;
        this.unitId = unitId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyRent = monthlyRent;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public int getUnitId() {
        return unitId;
    }

    public void setUnitId(int unitId) {
        this.unitId = unitId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Lease{id=" + id + ", tenantId=" + tenantId + ", unitId=" + unitId
                + ", startDate=" + startDate + ", endDate=" + endDate
                + ", monthlyRent=" + monthlyRent + ", status='" + status + "'}";
    }
}