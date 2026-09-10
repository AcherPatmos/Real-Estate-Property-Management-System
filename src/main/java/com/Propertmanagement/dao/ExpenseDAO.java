package com.Propertmanagement.dao;

import com.Propertmanagement.db.ConnectionManager;
import com.Propertmanagement.model.Expense;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

//Method function that has all the CRUD processes for Expenses
public class ExpenseDAO {

    public int createExpense(Expense expense) {
        validateExactlyOneOwner(expense);

        String sql = "INSERT INTO Expense (property_id, unit_id, description, amount, expense_date) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setObject(1, expense.getPropertyId());
            stmt.setObject(2, expense.getUnitId());
            stmt.setString(3, expense.getDescription());
            stmt.setBigDecimal(4, expense.getAmount());
            stmt.setDate(5, Date.valueOf(expense.getExpenseDate()));
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create expense.", e);
        }
    }

    public Expense getExpenseById(int id) {
        String sql = "SELECT id, property_id, unit_id, description, amount, expense_date "
                + "FROM Expense WHERE id = ?";

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
            throw new RuntimeException("Failed to fetch expense with id " + id, e);
        }
    }

    public List<Expense> getExpensesByPropertyId(int propertyId) {
        String sql = "SELECT id, property_id, unit_id, description, amount, expense_date "
                + "FROM Expense WHERE property_id = ? ORDER BY expense_date";
        List<Expense> expenses = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, propertyId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapRow(rs));
                }
            }
            return expenses;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch expenses for property " + propertyId, e);
        }
    }

    public List<Expense> getExpensesByUnitId(int unitId) {
        String sql = "SELECT id, property_id, unit_id, description, amount, expense_date "
                + "FROM Expense WHERE unit_id = ? ORDER BY expense_date";
        List<Expense> expenses = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, unitId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapRow(rs));
                }
            }
            return expenses;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch expenses for unit " + unitId, e);
        }
    }

    public List<Expense> getAllExpenses() {
        String sql = "SELECT id, property_id, unit_id, description, amount, expense_date "
                + "FROM Expense ORDER BY id";
        List<Expense> expenses = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                expenses.add(mapRow(rs));
            }
            return expenses;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch expenses.", e);
        }
    }

    public boolean updateExpense(Expense expense) {
        validateExactlyOneOwner(expense);

        String sql = "UPDATE Expense SET property_id = ?, unit_id = ?, description = ?, "
                + "amount = ?, expense_date = ? WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, expense.getPropertyId());
            stmt.setObject(2, expense.getUnitId());
            stmt.setString(3, expense.getDescription());
            stmt.setBigDecimal(4, expense.getAmount());
            stmt.setDate(5, Date.valueOf(expense.getExpenseDate()));
            stmt.setInt(6, expense.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update expense with id " + expense.getId(), e);
        }
    }

    public boolean deleteExpense(int id) {
        String sql = "DELETE FROM Expense WHERE id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete expense with id " + id, e);
        }
    }

    // Defensive check backing up the DB-level CHECK constraint (which only MySQL 8.0.16+
    // actually enforces) - exactly one of propertyId/unitId must be set, never both, never neither.
    private void validateExactlyOneOwner(Expense expense) {
        boolean hasProperty = expense.getPropertyId() != null;
        boolean hasUnit = expense.getUnitId() != null;

        if (hasProperty == hasUnit) {
            throw new IllegalArgumentException(
                    "Expense must belong to exactly one of Property or Unit, not both or neither.");
        }
    }

    private Expense mapRow(ResultSet rs) throws SQLException {
        return new Expense(
                rs.getInt("id"),
                rs.getObject("property_id", Integer.class),
                rs.getObject("unit_id", Integer.class),
                rs.getString("description"),
                rs.getBigDecimal("amount"),
                rs.getDate("expense_date").toLocalDate()
        );
    }
}