package com.Propertmanagement.dao;

import com.Propertmanagement.db.ConnectionManager;
import com.Propertmanagement.model.Floor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FloorDAO {

    public int createFloor(Floor floor) {
        String sql = "INSERT INTO Floor (building_id, floor_number) VALUES (?, ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, floor.getBuildingId());
            stmt.setInt(2, floor.getFloorNumber());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create floor.", e);
        }
    }

    public Floor getFloorById(int id) {
        String sql = "SELECT id, building_id, floor_number FROM Floor WHERE id = ?";

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
            throw new RuntimeException("Failed to fetch floor with id " + id, e);
        }
    }

    public List<Floor> getFloorsByBuildingId(int buildingId) {
        String sql = "SELECT id, building_id, floor_number FROM Floor WHERE building_id = ? ORDER BY floor_number";
        List<Floor> floors = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, buildingId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    floors.add(mapRow(rs));
                }
            }
            return floors;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch floors for building " + buildingId, e);
        }
    }

    public List<Floor> getAllFloors() {
        String sql = "SELECT id, building_id, floor_number FROM Floor ORDER BY id";
        List<Floor> floors = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                floors.add(mapRow(rs));
            }
            return floors;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch floors.", e);
        }
    }

    public boolean updateFloor(Floor floor) {
        String sql = "UPDATE Floor SET building_id = ?, floor_number = ? WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, floor.getBuildingId());
            stmt.setInt(2, floor.getFloorNumber());
            stmt.setInt(3, floor.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update floor with id " + floor.getId(), e);
        }
    }

    public boolean deleteFloor(int id) {
        // Unit rows beneath this floor are removed automatically by
        // ON DELETE CASCADE in the schema.
        String sql = "DELETE FROM Floor WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete floor with id " + id, e);
        }
    }

    private Floor mapRow(ResultSet rs) throws SQLException {
        return new Floor(
                rs.getInt("id"),
                rs.getInt("building_id"),
                rs.getInt("floor_number")
        );
    }
}