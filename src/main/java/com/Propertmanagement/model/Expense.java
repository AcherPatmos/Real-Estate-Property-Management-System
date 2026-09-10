package com.Propertmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Expense {

    private int id;
    private Integer propertyId; // nullable - set when this expense belongs to a Property
    private Integer unitId;     // nullable - set when this expense belongs to a Unit
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;

    public Expense(Integer propertyId, Integer unitId, String description,
                   BigDecimal amount, LocalDate expenseDate) {
        this.propertyId = propertyId;
        this.unitId = unitId;
        this.description = description;
        this.amount = amount;
        this.expenseDate = expenseDate;
    }

    public Expense(int id, Integer propertyId, Integer unitId, String description,
                   BigDecimal amount, LocalDate expenseDate) {
        this.id = id;
        this.propertyId = propertyId;
        this.unitId = unitId;
        this.description = description;
        this.amount = amount;
        this.expenseDate = expenseDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Integer propertyId) {
        this.propertyId = propertyId;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    // True when this expense is attached to a Property rather than a Unit.
    public boolean isPropertyLevel() {
        return propertyId != null;
    }

    @Override
    public String toString() {
        return "Expense{id=" + id + ", propertyId=" + propertyId + ", unitId=" + unitId
                + ", description='" + description + "', amount=" + amount
                + ", expenseDate=" + expenseDate + "}";
    }
}
