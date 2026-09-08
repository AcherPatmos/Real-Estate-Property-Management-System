package com.Propertmanagement.model;

import java.math.BigDecimal;

public class Unit {

    private int id;
    private int floorId;
    private String unitNumber;
    private String status; // "VACANT", "OCCUPIED", or "MAINTENANCE"
    private BigDecimal rentAmount;

    public Unit(int floorId, String unitNumber, String status, BigDecimal rentAmount) {
        this.floorId = floorId;
        this.unitNumber = unitNumber;
        this.status = status;
        this.rentAmount = rentAmount;
    }

    public Unit(int id, int floorId, String unitNumber, String status, BigDecimal rentAmount) {
        this.id = id;
        this.floorId = floorId;
        this.unitNumber = unitNumber;
        this.status = status;
        this.rentAmount = rentAmount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFloorId() {
        return floorId;
    }

    public void setFloorId(int floorId) {
        this.floorId = floorId;
    }

    public String getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(String unitNumber) {
        this.unitNumber = unitNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getRentAmount() {
        return rentAmount;
    }

    public void setRentAmount(BigDecimal rentAmount) {
        this.rentAmount = rentAmount;
    }

    @Override
    public String toString() {
        return "Unit{id=" + id + ", floorId=" + floorId + ", unitNumber='" + unitNumber +
                "', status='" + status + "', rentAmount=" + rentAmount + "}";
    }
}