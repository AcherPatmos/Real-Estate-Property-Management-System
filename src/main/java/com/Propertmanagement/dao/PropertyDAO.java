package com.Propertmanagement.dao;

import com.Propertmanagement.db.ConnectionManager;
import com.Propertmanagement.model.Property;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PropertyDAO {

    public int createProperty(Property property) {
        String sql = "INSERT INTO Property (name, address) VALUES (?, ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, property.getName());
            stmt.setString(2, property.getAddress());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create property.", e);
        }
    }

    public Property getPropertyById(int id) {
        String sql = "SELECT id, name, address FROM Property WHERE id = ?";

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
            throw new RuntimeException("Failed to fetch property with id " + id, e);
        }
    }

    public List<Property> getAllProperties() {
        String sql = "SELECT id, name, address FROM Property ORDER BY id";
        List<Property> properties = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                properties.add(mapRow(rs));
            }
            return properties;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch properties.", e);
        }
    }

    public boolean updateProperty(Property property) {
        String sql = "UPDATE Property SET name = ?, address = ? WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, property.getName());
            stmt.setString(2, property.getAddress());
            stmt.setInt(3, property.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update property with id " + property.getId(), e);
        }
    }

    public boolean deleteProperty(int id) {
        // Building/Floor/Unit rows beneath this property are removed
        // automatically by ON DELETE CASCADE in the schema.
        String sql = "DELETE FROM Property WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete property with id " + id, e);
        }
    }

    private Property mapRow(ResultSet rs) throws SQLException {
        return new Property(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("address")
        );
    }
}