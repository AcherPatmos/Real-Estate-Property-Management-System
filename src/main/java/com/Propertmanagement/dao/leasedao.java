package com.Propertmanagement.dao;

import com.Propertmanagement.db.ConnectionManager;
import com.Propertmanagement.model.Lease;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

//Method function that has all the CRUD processes for Leases
public class leasedao {

    public int createLease(Lease lease) {
        String sql = "INSERT INTO Lease (tenant_id, unit_id, start_date, end_date, monthly_rent, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, lease.getTenantId());
            stmt.setInt(2, lease.getUnitId());
            stmt.setDate(3, Date.valueOf(lease.getStartDate()));
            stmt.setDate(4, Date.valueOf(lease.getEndDate()));
            stmt.setBigDecimal(5, lease.getMonthlyRent());
            stmt.setString(6, lease.getStatus());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create lease.", e);
        }
    }

    public Lease getLeaseById(int id) {
        String sql = "SELECT id, tenant_id, unit_id, start_date, end_date, monthly_rent, status "
                + "FROM Lease WHERE id = ?";

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
            throw new RuntimeException("Failed to fetch lease with id " + id, e);
        }
    }

    public List<Lease> getLeasesByTenantId(int tenantId) {
        String sql = "SELECT id, tenant_id, unit_id, start_date, end_date, monthly_rent, status "
                + "FROM Lease WHERE tenant_id = ? ORDER BY start_date";
        List<Lease> leases = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tenantId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    leases.add(mapRow(rs));
                }
            }
            return leases;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch leases for tenant " + tenantId, e);
        }
    }

    public List<Lease> getLeasesByUnitId(int unitId) {
        String sql = "SELECT id, tenant_id, unit_id, start_date, end_date, monthly_rent, status "
                + "FROM Lease WHERE unit_id = ? ORDER BY start_date";
        List<Lease> leases = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, unitId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    leases.add(mapRow(rs));
                }
            }
            return leases;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch leases for unit " + unitId, e);
        }
    }

    public List<Lease> getAllLeases() {
        String sql = "SELECT id, tenant_id, unit_id, start_date, end_date, monthly_rent, status "
                + "FROM Lease ORDER BY id";
        List<Lease> leases = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                leases.add(mapRow(rs));
            }
            return leases;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch leases.", e);
        }
    }

    /**
     * Validation helper for the "no overlapping leases on a unit" rule.
     * Two date ranges overlap when existingStart <= newEnd AND existingEnd >= newStart.
     * Call this from the Lease Create/Update screen BEFORE calling createLease/updateLease,
     * and reject the save with a validation message if this returns true.
     *
     * excludeLeaseId: pass the lease's own id when checking during an UPDATE (so it doesn't
     * flag itself as overlapping with itself); pass -1 when checking during a CREATE.
     */
    public boolean hasOverlappingLease(int unitId, LocalDate startDate, LocalDate endDate, int excludeLeaseId) {
        String sql = "SELECT COUNT(*) FROM Lease "
                + "WHERE unit_id = ? AND id != ? AND start_date <= ? AND end_date >= ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, unitId);
            stmt.setInt(2, excludeLeaseId);
            stmt.setDate(3, Date.valueOf(endDate));
            stmt.setDate(4, Date.valueOf(startDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check overlapping leases for unit " + unitId, e);
        }
    }

    public boolean updateLease(Lease lease) {
        String sql = "UPDATE Lease SET tenant_id = ?, unit_id = ?, start_date = ?, end_date = ?, "
                + "monthly_rent = ?, status = ? WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, lease.getTenantId());
            stmt.setInt(2, lease.getUnitId());
            stmt.setDate(3, Date.valueOf(lease.getStartDate()));
            stmt.setDate(4, Date.valueOf(lease.getEndDate()));
            stmt.setBigDecimal(5, lease.getMonthlyRent());
            stmt.setString(6, lease.getStatus());
            stmt.setInt(7, lease.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update lease with id " + lease.getId(), e);
        }
    }

    public boolean deleteLease(int id) {
        // Payment rows for this lease are removed automatically once Payment's FK
        // to Lease is added with ON DELETE CASCADE.
        String sql = "DELETE FROM Lease WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete lease with id " + id, e);
        }
    }

    private Lease mapRow(ResultSet rs) throws SQLException {
        return new Lease(
                rs.getInt("id"),
                rs.getInt("tenant_id"),
                rs.getInt("unit_id"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getBigDecimal("monthly_rent"),
                rs.getString("status")
        );
    }
}