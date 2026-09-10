package com.Propertmanagement.dao;

import com.Propertmanagement.db.ConnectionManager;
import com.Propertmanagement.model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

//Method function that has all the CRUD processes for Payments
public class PaymentDAO {

    public int createPayment(Payment payment) {
        String sql = "INSERT INTO Payment (lease_id, amount, payment_date, method) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, payment.getLeaseId());
            stmt.setBigDecimal(2, payment.getAmount());
            stmt.setDate(3, Date.valueOf(payment.getPaymentDate()));
            stmt.setString(4, payment.getMethod());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create payment.", e);
        }
    }

    public Payment getPaymentById(int id) {
        String sql = "SELECT id, lease_id, amount, payment_date, method FROM Payment WHERE id = ?";

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
            throw new RuntimeException("Failed to fetch payment with id " + id, e);
        }
    }

    public List<Payment> getPaymentsByLeaseId(int leaseId) {
        String sql = "SELECT id, lease_id, amount, payment_date, method FROM Payment "
                + "WHERE lease_id = ? ORDER BY payment_date";
        List<Payment> payments = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, leaseId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRow(rs));
                }
            }
            return payments;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch payments for lease " + leaseId, e);
        }
    }

    public List<Payment> getAllPayments() {
        String sql = "SELECT id, lease_id, amount, payment_date, method FROM Payment ORDER BY id";
        List<Payment> payments = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                payments.add(mapRow(rs));
            }
            return payments;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch payments.", e);
        }
    }

    // Convenience for the Payment screen / lease summary view: total paid so far on a lease.
    public BigDecimal getTotalPaidForLease(int leaseId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM Payment WHERE lease_id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, leaseId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
            return BigDecimal.ZERO;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to total payments for lease " + leaseId, e);
        }
    }

    public boolean updatePayment(Payment payment) {
        String sql = "UPDATE Payment SET lease_id = ?, amount = ?, payment_date = ?, method = ? WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payment.getLeaseId());
            stmt.setBigDecimal(2, payment.getAmount());
            stmt.setDate(3, Date.valueOf(payment.getPaymentDate()));
            stmt.setString(4, payment.getMethod());
            stmt.setInt(5, payment.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update payment with id " + payment.getId(), e);
        }
    }

    public boolean deletePayment(int id) {
        String sql = "DELETE FROM Payment WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete payment with id " + id, e);
        }
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        return new Payment(
                rs.getInt("id"),
                rs.getInt("lease_id"),
                rs.getBigDecimal("amount"),
                rs.getDate("payment_date").toLocalDate(),
                rs.getString("method")
        );
    }
}