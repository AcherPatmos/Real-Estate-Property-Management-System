package com.Propertmanagement.dao;

import com.Propertmanagement.db.ConnectionManager;
import com.Propertmanagement.model.MaintenanceStats;
import com.Propertmanagement.model.MaintenanceTask;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

//Method function that has all the CRUD processes for MaintenanceTasks,
//plus the recursive hierarchy traversal (computeStats).
public class MaintenanceTaskDAO {

    public int createTask(MaintenanceTask task) {
        String sql = "INSERT INTO MaintenanceTask (maintenance_request_id, parent_task_id, title, cost, status) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, task.getMaintenanceRequestId());
            stmt.setObject(2, task.getParentTaskId());
            stmt.setString(3, task.getTitle());
            stmt.setBigDecimal(4, task.getCost());
            stmt.setString(5, task.getStatus());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create maintenance task.", e);
        }
    }

    public MaintenanceTask getTaskById(int id) {
        String sql = "SELECT id, maintenance_request_id, parent_task_id, title, cost, status "
                + "FROM MaintenanceTask WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch maintenance task with id " + id, e);
        }
    }

    // Top-level tasks directly under a MaintenanceRequest (parent_task_id IS NULL).
    public List<MaintenanceTask> getTopLevelTasksByRequestId(int requestId) {
        String sql = "SELECT id, maintenance_request_id, parent_task_id, title, cost, status "
                + "FROM MaintenanceTask WHERE maintenance_request_id = ? AND parent_task_id IS NULL "
                + "ORDER BY id";
        List<MaintenanceTask> tasks = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requestId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRow(rs));
                }
            }
            return tasks;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch top-level tasks for request " + requestId, e);
        }
    }

    // Direct children (one level down) of a given task - the building block the
    // recursion below calls at every level, regardless of how deep it goes.
    public List<MaintenanceTask> getSubtasks(int parentTaskId) {
        String sql = "SELECT id, maintenance_request_id, parent_task_id, title, cost, status "
                + "FROM MaintenanceTask WHERE parent_task_id = ? ORDER BY id";
        List<MaintenanceTask> tasks = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, parentTaskId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRow(rs));
                }
            }
            return tasks;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch subtasks for task " + parentTaskId, e);
        }
    }

    public List<MaintenanceTask> getAllTasks() {
        String sql = "SELECT id, maintenance_request_id, parent_task_id, title, cost, status "
                + "FROM MaintenanceTask ORDER BY id";
        List<MaintenanceTask> tasks = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tasks.add(mapRow(rs));
            }
            return tasks;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch maintenance tasks.", e);
        }
    }

    public boolean updateTask(MaintenanceTask task) {
        String sql = "UPDATE MaintenanceTask SET maintenance_request_id = ?, parent_task_id = ?, "
                + "title = ?, cost = ?, status = ? WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, task.getMaintenanceRequestId());
            stmt.setObject(2, task.getParentTaskId());
            stmt.setString(3, task.getTitle());
            stmt.setBigDecimal(4, task.getCost());
            stmt.setString(5, task.getStatus());
            stmt.setInt(6, task.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update maintenance task with id " + task.getId(), e);
        }
    }

    public boolean deleteTask(int id) {
        // Subtasks beneath this task are removed automatically by ON DELETE CASCADE
        // on the self-referencing parent_task_id foreign key.
        String sql = "DELETE FROM MaintenanceTask WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete maintenance task with id " + id, e);
        }
    }

    /**
     * THE RECURSIVE FEATURE.
     * Walks the subtree rooted at taskId (this task plus every descendant
     * subtask/sub-subtask/...) and folds it into one MaintenanceStats: total
     * cost, completed count, and outstanding count across the whole subtree.
     *
     * Base case: a task with no subtasks - its own cost/status is the entire result.
     * Recursive case: fetch this task's direct children, recurse into each one,
     * and combine all of their results with this task's own contribution.
     *
     * To get stats for an ENTIRE MaintenanceRequest (all its top-level tasks and
     * everything beneath them), call this once per top-level task (from
     * getTopLevelTasksByRequestId) and combine the results with .plus(...).
     */
    public MaintenanceStats computeStats(int taskId) {
        MaintenanceTask task = getTaskById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("No maintenance task with id " + taskId);
        }

        // This node's own contribution.
        MaintenanceStats ownStats = new MaintenanceStats(
                task.getCost(),
                task.isCompleted() ? 1 : 0,
                task.isCompleted() ? 0 : 1
        );

        List<MaintenanceTask> children = getSubtasks(taskId);

        // Base case: no children - stop recursing, this node's own stats are the whole answer.
        if (children.isEmpty()) {
            return ownStats;
        }

        // Recursive case: fold in every child subtree's stats (each call to
        // computeStats goes one level deeper, until it hits a base case above).
        MaintenanceStats combined = ownStats;
        for (MaintenanceTask child : children) {
            combined = combined.plus(computeStats(child.getId()));
        }
        return combined;
    }

    private MaintenanceTask mapRow(ResultSet rs) throws SQLException {
        return new MaintenanceTask(
                rs.getInt("id"),
                rs.getInt("maintenance_request_id"),
                rs.getObject("parent_task_id", Integer.class),
                rs.getString("title"),
                rs.getBigDecimal("cost"),
                rs.getString("status")
        );
    }
}
