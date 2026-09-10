package com.Propertmanagement.dao;

import com.Propertmanagement.db.ConnectionManager;
import com.Propertmanagement.model.Tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

//Method function that has all the CRUD processes for Tenants
public class TenantDAO {

    public int createTenant(Tenant tenant) {
        String sql = "INSERT INTO Tenant (first_name, last_name, email, phone) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, tenant.getFirstName());
            stmt.setString(2, tenant.getLastName());
            stmt.setString(3, tenant.getEmail());
            stmt.setString(4, tenant.getPhone());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tenant.", e);
        }
    }

    public Tenant getTenantById(int id) {
        String sql = "SELECT id, first_name, last_name, email, phone FROM Tenant WHERE id = ?";

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
            throw new RuntimeException("Failed to fetch tenant with id " + id, e);
        }
    }

    public List<Tenant> getAllTenants() {
        String sql = "SELECT id, first_name, last_name, email, phone FROM Tenant ORDER BY id";
        List<Tenant> tenants = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tenants.add(mapRow(rs));
            }
            return tenants;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch tenants.", e);
        }
    }

    public boolean updateTenant(Tenant tenant) {
        String sql = "UPDATE Tenant SET first_name = ?, last_name = ?, email = ?, phone = ? WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tenant.getFirstName());
            stmt.setString(2, tenant.getLastName());
            stmt.setString(3, tenant.getEmail());
            stmt.setString(4, tenant.getPhone());
            stmt.setInt(5, tenant.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update tenant with id " + tenant.getId(), e);
        }
    }

    public boolean deleteTenant(int id) {
        // If this tenant has Lease rows, add ON DELETE behavior on the Lease FK
        // (cascade, or restrict + check here) once Lease exists.
        String sql = "DELETE FROM Tenant WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete tenant with id " + id, e);
        }
    }

    private Tenant mapRow(ResultSet rs) throws SQLException {
        return new Tenant(
                rs.getInt("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone")
        );
    }
}