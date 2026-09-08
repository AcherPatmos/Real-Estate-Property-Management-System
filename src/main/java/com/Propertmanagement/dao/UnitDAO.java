package com.Propertmanagement.dao;

import com.Propertmanagement.db.ConnectionManager;
import com.Propertmanagement.model.Unit;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UnitDAO {

    public int createUnit(Unit unit) {
        String sql = "INSERT INTO Unit (floor_id, unit_number, status, rent_amount) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, unit.getFloorId());
            stmt.setString(2, unit.getUnitNumber());
            stmt.setString(3, unit.getStatus());
            stmt.setBigDecimal(4, unit.getRentAmount());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create unit.", e);
        }
    }

    public Unit getUnitById(int id) {
        String sql = "SELECT id, floor_id, unit_number, status, rent_amount FROM Unit WHERE id = ?";

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
            throw new RuntimeException("Failed to fetch unit with id " + id, e);
        }
    }

    public List<Unit> getUnitsByFloorId(int floorId) {
        String sql = "SELECT id, floor_id, unit_number, status, rent_amount FROM Unit WHERE floor_id = ? ORDER BY unit_number";
        List<Unit> units = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, floorId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    units.add(mapRow(rs));
                }
            }
            return units;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch units for floor " + floorId, e);
        }
    }

    public List<Unit> getAllUnits() {
        String sql = "SELECT id, floor_id, unit_number, status, rent_amount FROM Unit ORDER BY id";
        List<Unit> units = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                units.add(mapRow(rs));
            }
            return units;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch units.", e);
        }
    }

    public boolean updateUnit(Unit unit) {
        String sql = "UPDATE Unit SET floor_id = ?, unit_number = ?, status = ?, rent_amount = ? WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, unit.getFloorId());
            stmt.setString(2, unit.getUnitNumber());
            stmt.setString(3, unit.getStatus());
            stmt.setBigDecimal(4, unit.getRentAmount());
            stmt.setInt(5, unit.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update unit with id " + unit.getId(), e);
        }
    }

    public boolean deleteUnit(int id) {
        String sql = "DELETE FROM Unit WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete unit with id " + id, e);
        }
    }

    private Unit mapRow(ResultSet rs) throws SQLException {
        BigDecimal rent = rs.getBigDecimal("rent_amount");
        return new Unit(
                rs.getInt("id"),
                rs.getInt("floor_id"),
                rs.getString("unit_number"),
                rs.getString("status"),
                rent
        );
    }
}