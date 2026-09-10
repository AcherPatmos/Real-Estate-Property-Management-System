package com.Propertmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Payment {

    private int id;
    private int leaseId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String method;

    public Payment(int leaseId, BigDecimal amount, LocalDate paymentDate, String method) {
        this.leaseId = leaseId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.method = method;
    }

    public Payment(int id, int leaseId, BigDecimal amount, LocalDate paymentDate, String method) {
        this.id = id;
        this.leaseId = leaseId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.method = method;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLeaseId() {
        return leaseId;
    }

    public void setLeaseId(int leaseId) {
        this.leaseId = leaseId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "Payment{id=" + id + ", leaseId=" + leaseId + ", amount=" + amount
                + ", paymentDate=" + paymentDate + ", method='" + method + "'}";
    }
}
