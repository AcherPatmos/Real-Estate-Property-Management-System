package com.Propertmanagement.dao;

import com.Propertmanagement.model.MaintenanceRequest;
import com.Propertmanagement.model.MaintenanceStats;
import com.Propertmanagement.model.MaintenanceTask;
import com.Propertmanagement.model.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests the recursive computeStats() traversal: a leaf task (base case), a
// two-level tree (one subtask), and a three-level tree (subtask + sub-subtask).
// Runs against the real dev database; each test cleans up after itself by
// deleting the Property it created, which cascades down through
// MaintenanceRequest -> MaintenanceTask -> subtasks automatically.
class MaintenanceTaskDAOTest {

    private final PropertyDAO propertyDAO = new PropertyDAO();
    private final MaintenanceRequestDAO requestDAO = new MaintenanceRequestDAO();
    private final MaintenanceTaskDAO taskDAO = new MaintenanceTaskDAO();

    private int propertyId;
    private int requestId;

    @BeforeEach
    void setUp() {
        propertyId = propertyDAO.createProperty(new Property("Test Property", "1 Test St"));
        requestId = requestDAO.createRequest(
                new MaintenanceRequest(propertyId, null, "Test Request", null, "OPEN"));
    }

    @AfterEach
    void tearDown() {
        // Cascades: Property -> MaintenanceRequest -> MaintenanceTask -> subtasks.
        propertyDAO.deleteProperty(propertyId);
    }

    @Test
    void singleLeafTask_baseCase_returnsOwnStatsOnly() {
        int taskId = taskDAO.createTask(
                new MaintenanceTask(requestId, null, "Fix tap", new BigDecimal("50.00"), "COMPLETED"));

        MaintenanceStats stats = taskDAO.computeStats(taskId);

        assertEquals(0, new BigDecimal("50.00").compareTo(stats.getTotalCost()));
        assertEquals(1, stats.getCompletedCount());
        assertEquals(0, stats.getOutstandingCount());
        assertEquals(100.0, stats.getCompletionPercentage(), 0.001);
    }

    @Test
    void twoLevelTree_parentPlusOneSubtask_combinesBoth() {
        int parentId = taskDAO.createTask(
                new MaintenanceTask(requestId, null, "Rewire kitchen", new BigDecimal("200.00"), "PENDING"));
        taskDAO.createTask(
                new MaintenanceTask(requestId, parentId, "Buy cable", new BigDecimal("30.00"), "COMPLETED"));

        MaintenanceStats stats = taskDAO.computeStats(parentId);

        // 200.00 (parent) + 30.00 (subtask) = 230.00
        assertEquals(0, new BigDecimal("230.00").compareTo(stats.getTotalCost()));
        assertEquals(1, stats.getCompletedCount());   // the subtask
        assertEquals(1, stats.getOutstandingCount());  // the parent itself
        assertEquals(50.0, stats.getCompletionPercentage(), 0.001); // 1 of 2 done
    }

    @Test
    void threeLevelTree_taskSubtaskSubSubtask_sumsAllLevels() {
        int taskId = taskDAO.createTask(
                new MaintenanceTask(requestId, null, "Renovate bathroom", new BigDecimal("500.00"), "PENDING"));
        int subtaskId = taskDAO.createTask(
                new MaintenanceTask(requestId, taskId, "Replace tiling", new BigDecimal("150.00"), "COMPLETED"));
        taskDAO.createTask(
                new MaintenanceTask(requestId, subtaskId, "Buy grout", new BigDecimal("10.00"), "COMPLETED"));

        MaintenanceStats stats = taskDAO.computeStats(taskId);

        // 500.00 + 150.00 + 10.00 = 660.00 across all three levels
        assertEquals(0, new BigDecimal("660.00").compareTo(stats.getTotalCost()));
        assertEquals(2, stats.getCompletedCount());    // subtask + sub-subtask
        assertEquals(1, stats.getOutstandingCount());   // the top task itself
        assertEquals(3, stats.getTotalCount());
        assertTrue(stats.getCompletionPercentage() > 66.0 && stats.getCompletionPercentage() < 67.0); // 2/3
    }
}
