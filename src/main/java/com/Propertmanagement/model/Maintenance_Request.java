package com.Propertmanagement.model;

public class Maintenance_Request {

    private int id;
    private Integer propertyId; // nullable - set when this request belongs to a Property
    private Integer unitId;     // nullable - set when this request belongs to a Unit
    private String title;
    private String description;
    private String status;

    public Maintenance_Request(Integer propertyId, Integer unitId, String title,
                              String description, String status) {
        this.propertyId = propertyId;
        this.unitId = unitId;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    public Maintenance_Request(int id, Integer propertyId, Integer unitId, String title,
                              String description, String status) {
        this.id = id;
        this.propertyId = propertyId;
        this.unitId = unitId;
        this.title = title;
        this.description = description;
        this.status = status;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "MaintenanceRequest{id=" + id + ", propertyId=" + propertyId + ", unitId=" + unitId
                + ", title='" + title + "', status='" + status + "'}";
    }
}