package com.Propertmanagement.model;

import java.math.BigDecimal;

public class MaintenanceTask {

    private int id;
    private int maintenanceRequestId;
    private Integer parentTaskId; // nullable - null means this is a top-level Task, not a Subtask
    private String title;
    private BigDecimal cost;
    private String status; // e.g. "PENDING" or "COMPLETED"

    public MaintenanceTask(int maintenanceRequestId, Integer parentTaskId, String title,
                            BigDecimal cost, String status) {
        this.maintenanceRequestId = maintenanceRequestId;
        this.parentTaskId = parentTaskId;
        this.title = title;
        this.cost = cost;
        this.status = status;
    }

    public MaintenanceTask(int id, int maintenanceRequestId, Integer parentTaskId, String title,
                            BigDecimal cost, String status) {
        this.id = id;
        this.maintenanceRequestId = maintenanceRequestId;
        this.parentTaskId = parentTaskId;
        this.title = title;
        this.cost = cost;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMaintenanceRequestId() {
        return maintenanceRequestId;
    }

    public void setMaintenanceRequestId(int maintenanceRequestId) {
        this.maintenanceRequestId = maintenanceRequestId;
    }

    public Integer getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Integer parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    public boolean isTopLevel() {
        return parentTaskId == null;
    }

    @Override
    public String toString() {
        return "MaintenanceTask{id=" + id + ", requestId=" + maintenanceRequestId
                + ", parentTaskId=" + parentTaskId + ", title='" + title
                + "', cost=" + cost + ", status='" + status + "'}";
    }
}
