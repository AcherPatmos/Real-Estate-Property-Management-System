package com.Propertmanagement.dao;

import com.Propertmanagement.db.ConnectionManager;
import com.Propertmanagement.model.Maintenance_Request;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

//Method function that has all the CRUD processes for MaintenanceRequests
public class Maintenance_RequestDAO {

    public int createRequest(Maintenance_Request request) {
        validateExactlyOneOwner(request);

        String sql = "INSERT INTO MaintenanceRequest (property_id, unit_id, title, description, status) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setObject(1, request.getPropertyId());
            stmt.setObject(2, request.getUnitId());
            stmt.setString(3, request.getTitle());
            stmt.setString(4, request.getDescription());
            stmt.setString(5, request.getStatus());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create maintenance request.", e);
        }
    }

    public Maintenance_Request getRequestById(int id) {
        String sql = "SELECT id, property_id, unit_id, title, description, status "
                + "FROM MaintenanceRequest WHERE id = ?";

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
            throw new RuntimeException("Failed to fetch maintenance request with id " + id, e);
        }
    }

    public List<Maintenance_Request> getRequestsByPropertyId(int propertyId) {
        String sql = "SELECT id, property_id, unit_id, title, description, status "
                + "FROM MaintenanceRequest WHERE property_id = ? ORDER BY id";
        List<Maintenance_Request> requests = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, propertyId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapRow(rs));
                }
            }
            return requests;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch maintenance requests for property " + propertyId, e);
        }
    }

    public List<Maintenance_Request> getRequestsByUnitId(int unitId) {
        String sql = "SELECT id, property_id, unit_id, title, description, status "
                + "FROM MaintenanceRequest WHERE unit_id = ? ORDER BY id";
        List<Maintenance_Request> requests = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, unitId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapRow(rs));
                }
            }
            return requests;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch maintenance requests for unit " + unitId, e);
        }
    }

    public List<Maintenance_Request> getAllRequests() {
        String sql = "SELECT id, property_id, unit_id, title, description, status "
                + "FROM MaintenanceRequest ORDER BY id";
        List<Maintenance_Request> requests = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                requests.add(mapRow(rs));
            }
            return requests;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch maintenance requests.", e);
        }
    }

    public boolean updateRequest(Maintenance_Request request) {
        validateExactlyOneOwner(request);

        String sql = "UPDATE MaintenanceRequest SET property_id = ?, unit_id = ?, title = ?, "
                + "description = ?, status = ? WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, request.getPropertyId());
            stmt.setObject(2, request.getUnitId());
            stmt.setString(3, request.getTitle());
            stmt.setString(4, request.getDescription());
            stmt.setString(5, request.getStatus());
            stmt.setInt(6, request.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update maintenance request with id " + request.getId(), e);
        }
    }

    public boolean deleteRequest(int id) {
        // MaintenanceTask rows under this request are removed automatically
        // by ON DELETE CASCADE once MaintenanceTask's FK to this table is added.
        String sql = "DELETE FROM MaintenanceRequest WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete maintenance request with id " + id, e);
        }
    }

    private void validateExactlyOneOwner(Maintenance_Request request) {
        boolean hasProperty = request.getPropertyId() != null;
        boolean hasUnit = request.getUnitId() != null;

        if (hasProperty == hasUnit) {
            throw new IllegalArgumentException(
                    "MaintenanceRequest must belong to exactly one of Property or Unit, not both or neither.");
        }
    }

    private Maintenance_Request mapRow(ResultSet rs) throws SQLException {
        return new Maintenance_Request(
                rs.getInt("id"),
                rs.getObject("property_id", Integer.class),
                rs.getObject("unit_id", Integer.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("status")
        );
    }
}