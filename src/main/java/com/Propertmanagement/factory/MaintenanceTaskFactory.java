package com.Propertmanagement.factory;

import com.Propertmanagement.dao.MaintenanceTaskDAO;
import com.Propertmanagement.model.MaintenanceTask;

import java.math.BigDecimal;

/**
 * Factory Method pattern: centralizes the rules for constructing a
 * MaintenanceTask depending on what kind it is - a top-level Task directly
 * under a MaintenanceRequest, or a Subtask nested under an existing task.
 *
 * Without this factory, callers (e.g. MaintenancePanel) would need to
 * remember several rules themselves every time they build a MaintenanceTask:
 * - a top-level task's parentTaskId must be null
 * - a subtask's parentTaskId must point at a real, existing task
 * - a subtask must belong to the SAME maintenance_request_id as its parent
 *   (never a different request than the one its parent belongs to)
 * - new tasks always start out as "PENDING", never pre-marked "COMPLETED"
 *
 * Centralizing this here means those rules are enforced in exactly one
 * place, and can be tested independently of any GUI code.
 */
public class MaintenanceTaskFactory {

    private static final String DEFAULT_STATUS = "PENDING";

    private final MaintenanceTaskDAO taskDAO;

    public MaintenanceTaskFactory(MaintenanceTaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    // Factory method 1: a top-level Task directly under a MaintenanceRequest (no parent).
    public MaintenanceTask createTopLevelTask(int maintenanceRequestId, String title, BigDecimal cost) {
        return new MaintenanceTask(maintenanceRequestId, null, title, cost, DEFAULT_STATUS);
    }

    // Factory method 2: a Subtask nested under an existing task. Looks up the parent
    // so the subtask automatically inherits the correct maintenanceRequestId - the
    // caller never has to pass (or risk mismatching) it manually.
    public MaintenanceTask createSubtask(int parentTaskId, String title, BigDecimal cost) {
        MaintenanceTask parent = taskDAO.getTaskById(parentTaskId);
        if (parent == null) {
            throw new IllegalArgumentException(
                    "No maintenance task with id " + parentTaskId + " to attach a subtask to.");
        }
        return new MaintenanceTask(parent.getMaintenanceRequestId(), parentTaskId, title, cost, DEFAULT_STATUS);
    }
}
