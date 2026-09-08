package com.Propertmanagement.model;

public class Building {

    private int id;
    private int propertyId;
    private String name;

    public Building(int propertyId, String name) {
        this.propertyId = propertyId;
        this.name = name;
    }

    public Building(int id, int propertyId, String name) {
        this.id = id;
        this.propertyId = propertyId;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(int propertyId) {
        this.propertyId = propertyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Building{id=" + id + ", propertyId=" + propertyId + ", name='" + name + "'}";
    }
}