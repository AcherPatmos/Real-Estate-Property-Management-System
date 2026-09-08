package com.Propertmanagement.dao;

import com.Propertmanagement.db.ConnectionManager;
import com.Propertmanagement.model.Building;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

//Method function that has all the CRUDE processes for Buildings
public class BuildingDAO {

    public int createBuilding(Building building) {
        String sql = "INSERT INTO Building (property_id, name) VALUES (?, ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, building.getPropertyId());
            stmt.setString(2, building.getName());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create building.", e);
        }
    }

    public Building getBuildingById(int id) {
        String sql = "SELECT id, property_id, name FROM Building WHERE id = ?";

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
            throw new RuntimeException("Failed to fetch building with id " + id, e);
        }
    }

    public List<Building> getBuildingsByPropertyId(int propertyId) {
        String sql = "SELECT id, property_id, name FROM Building WHERE property_id = ? ORDER BY id";
        List<Building> buildings = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, propertyId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    buildings.add(mapRow(rs));
                }
            }
            return buildings;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch buildings for property " + propertyId, e);
        }
    }

    public List<Building> getAllBuildings() {
        String sql = "SELECT id, property_id, name FROM Building ORDER BY id";
        List<Building> buildings = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                buildings.add(mapRow(rs));
            }
            return buildings;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch buildings.", e);
        }
    }

    public boolean updateBuilding(Building building) {
        String sql = "UPDATE Building SET property_id = ?, name = ? WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, building.getPropertyId());
            stmt.setString(2, building.getName());
            stmt.setInt(3, building.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update building with id " + building.getId(), e);
        }
    }

    public boolean deleteBuilding(int id) {
        // Floor/Unit rows beneath this building are removed automatically
        // by ON DELETE CASCADE in the schema.
        String sql = "DELETE FROM Building WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete building with id " + id, e);
        }
    }

    private Building mapRow(ResultSet rs) throws SQLException {
        return new Building(
                rs.getInt("id"),
                rs.getInt("property_id"),
                rs.getString("name")
        );
    }
}
