package com.Propertmanagement.model;

public class Floor {

    private int id;
    private int buildingId;
    private int floorNumber;

    public Floor(int buildingId, int floorNumber) {
        this.buildingId = buildingId;
        this.floorNumber = floorNumber;
    }

    public Floor(int id, int buildingId, int floorNumber) {
        this.id = id;
        this.buildingId = buildingId;
        this.floorNumber = floorNumber;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    @Override
    public String toString() {
        return "Floor{id=" + id + ", buildingId=" + buildingId + ", floorNumber=" + floorNumber + "}";
    }
}