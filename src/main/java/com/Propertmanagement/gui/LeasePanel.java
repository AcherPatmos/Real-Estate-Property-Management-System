package com.Propertmanagement.gui;

import com.Propertmanagement.dao.leasedao;
import com.Propertmanagement.dao.TenantDAO;
import com.Propertmanagement.dao.UnitDAO;
import com.Propertmanagement.model.Lease;
import com.Propertmanagement.model.Tenant;
import com.Propertmanagement.model.Unit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// CRUD screen for Lease. Two FK dropdowns (Tenant, Unit), plain-text date fields
// (yyyy-MM-dd, parsed manually - no date-picker component available), and the
// "no overlapping leases on a unit" check run before every create/update.
public class LeasePanel extends JPanel {

    private final leasedao leaseDAO;
    private final TenantDAO tenantDAO;
    private final UnitDAO unitDAO;

    private final JComboBox<TenantOption> tenantCombo = new JComboBox<>();
    private final JComboBox<UnitOption> unitCombo = new JComboBox<>();
    private final JTextField startDateField = new JTextField();
    private final JTextField endDateField = new JTextField();
    private final JTextField monthlyRentField = new JTextField();
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "ENDED"});

    private final JLabel tenantError = errorLabel();
    private final JLabel unitError = errorLabel();
    private final JLabel startDateError = errorLabel();
    private final JLabel endDateError = errorLabel();
    private final JLabel rentError = errorLabel();
    private final JLabel statusLabel = new JLabel(" ");

    private final LeaseTableModel tableModel;
    private final JTable table;

    private final JButton saveButton = new JButton("Save");
    private final JButton newButton = new JButton("New");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton refreshListsButton = new JButton("Refresh tenants/units");

    private Integer editingId = null;

    public LeasePanel(leasedao leaseDAO, TenantDAO tenantDAO, UnitDAO unitDAO) {
        super(new BorderLayout(12, 12));
        this.leaseDAO = leaseDAO;
        this.tenantDAO = tenantDAO;
        this.unitDAO = unitDAO;
        this.tableModel = new LeaseTableModel();
        this.table = new JTable(tableModel);

        setBorder(new EmptyBorder(16, 16, 16, 16));
        add(buildForm(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onRowSelected();
            }
        });

        newButton.addActionListener(e -> clearForm());
        saveButton.addActionListener(e -> onSave());
        deleteButton.addActionListener(e -> onDelete());
        refreshListsButton.addActionListener(e -> reloadDropdowns());

        reloadDropdowns(); // populate FK dropdowns before the table, same ordering as BuildingPanel
        reload();
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Lease");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        form.add(heading, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Tenant:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(tenantCombo, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        form.add(refreshListsButton, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(tenantError, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        form.add(new JLabel("Unit:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(unitCombo, gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        form.add(unitError, gbc);
        gbc.gridwidth = 1;

        addFieldRow(form, gbc, 5, "Start Date (yyyy-MM-dd)", startDateField, startDateError);
        addFieldRow(form, gbc, 7, "End Date (yyyy-MM-dd)", endDateField, endDateError);
        addFieldRow(form, gbc, 9, "Monthly Rent", monthlyRentField, rentError);

        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.weightx = 0;
        form.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(statusCombo, gbc);
        gbc.gridwidth = 1;

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(saveButton);
        buttons.add(newButton);
        buttons.add(deleteButton);
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.gridwidth = 3;
        form.add(buttons, gbc);

        gbc.gridy = 13;
        statusLabel.setForeground(new Color(184, 36, 36));
        form.add(statusLabel, gbc);

        return form;
    }

    private void addFieldRow(JPanel form, GridBagConstraints gbc, int row, String labelText,
                             JTextField field, JLabel errorLabel) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        form.add(new JLabel(labelText + ":"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        field.setColumns(24);
        form.add(field, gbc);

        gbc.gridx = 1;
        gbc.gridy = row + 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(errorLabel, gbc);
        gbc.gridwidth = 1;
    }

    private static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(new Color(178, 34, 34));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        return label;
    }

    private JComponent buildTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        return new JScrollPane(table);
    }

    // Re-fetches Tenants and Units and rebuilds both dropdowns, restoring prior selections by ID if still present.
    private void reloadDropdowns() {
        TenantOption previousTenant = (TenantOption) tenantCombo.getSelectedItem();
        UnitOption previousUnit = (UnitOption) unitCombo.getSelectedItem();

        tenantCombo.removeAllItems();
        unitCombo.removeAllItems();
        tableModel.clearLabelCaches();

        try {
            List<Tenant> tenants = tenantDAO.getAllTenants();
            for (Tenant t : tenants) {
                tenantCombo.addItem(new TenantOption(t));
            }
            List<Unit> units = unitDAO.getAllUnits();
            for (Unit u : units) {
                unitCombo.addItem(new UnitOption(u));
            }

            if (tenants.isEmpty() || units.isEmpty()) {
                setStatus("Add at least one tenant and one unit before creating a lease.");
            } else {
                setStatus(null);
            }

            restoreSelection(tenantCombo, previousTenant, o -> ((TenantOption) o).tenant.getId());
            restoreSelection(unitCombo, previousUnit, o -> ((UnitOption) o).unit.getId());

        } catch (RuntimeException e) {
            setStatus("Could not load tenants/units: " + rootMessage(e));
        }
    }

    private interface IdExtractor {
        int idOf(Object comboOption);
    }

    private void restoreSelection(JComboBox<?> combo, Object previous, IdExtractor idExtractor) {
        if (previous == null) {
            return;
        }
        int previousId = idExtractor.idOf(previous);
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (idExtractor.idOf(combo.getItemAt(i)) == previousId) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        Lease selected = tableModel.getRowAt(table.convertRowIndexToModel(row));
        editingId = selected.getId();
        selectComboById(tenantCombo, selected.getTenantId());
        selectComboById(unitCombo, selected.getUnitId());
        startDateField.setText(selected.getStartDate().toString());
        endDateField.setText(selected.getEndDate().toString());
        monthlyRentField.setText(selected.getMonthlyRent().toPlainString());
        statusCombo.setSelectedItem(selected.getStatus());
        clearErrors();
        setStatus(null);
    }

    private void selectComboById(JComboBox<?> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            int itemId = (item instanceof TenantOption)
                    ? ((TenantOption) item).tenant.getId()
                    : ((UnitOption) item).unit.getId();
            if (itemId == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void clearForm() {
        editingId = null;
        if (tenantCombo.getItemCount() > 0) {
            tenantCombo.setSelectedIndex(0);
        }
        if (unitCombo.getItemCount() > 0) {
            unitCombo.setSelectedIndex(0);
        }
        startDateField.setText("");
        endDateField.setText("");
        monthlyRentField.setText("");
        statusCombo.setSelectedItem("ACTIVE");
        table.clearSelection();
        clearErrors();
        setStatus(null);
    }

    private void clearErrors() {
        tenantError.setText(" ");
        unitError.setText(" ");
        startDateError.setText(" ");
        endDateError.setText(" ");
        rentError.setText(" ");
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null ? " " : message);
    }

    private void onSave() {
        clearErrors();
        boolean valid = true;

        TenantOption selectedTenant = (TenantOption) tenantCombo.getSelectedItem();
        if (selectedTenant == null) {
            tenantError.setText("Choose a tenant — add one first if none are listed.");
            valid = false;
        }

        UnitOption selectedUnit = (UnitOption) unitCombo.getSelectedItem();
        if (selectedUnit == null) {
            unitError.setText("Choose a unit — add one first if none are listed.");
            valid = false;
        }

        LocalDate startDate = null;
        try {
            startDate = LocalDate.parse(startDateField.getText().trim());
        } catch (DateTimeParseException e) {
            startDateError.setText("Enter a valid date as yyyy-MM-dd.");
            valid = false;
        }

        LocalDate endDate = null;
        try {
            endDate = LocalDate.parse(endDateField.getText().trim());
        } catch (DateTimeParseException e) {
            endDateError.setText("Enter a valid date as yyyy-MM-dd.");
            valid = false;
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            endDateError.setText("End date must be on or after the start date.");
            valid = false;
        }

        BigDecimal monthlyRent = null;
        try {
            monthlyRent = new BigDecimal(monthlyRentField.getText().trim());
            if (monthlyRent.signum() < 0) {
                rentError.setText("Monthly rent cannot be negative.");
                valid = false;
            }
        } catch (NumberFormatException e) {
            rentError.setText("Enter a valid amount, e.g. 450.00");
            valid = false;
        }

        if (!valid) {
            return;
        }

        try {
            // Overlap check happens here, before anything is written - this is the
            // "no overlapping leases on a unit" rule from the spec, enforced by the
            // GUI calling LeaseDAO.hasOverlappingLease before create/update.
            int excludeId = (editingId == null) ? -1 : editingId;
            if (leaseDAO.hasOverlappingLease(selectedUnit.unit.getId(), startDate, endDate, excludeId)) {
                unitError.setText("This unit already has a lease overlapping these dates.");
                return;
            }

            if (editingId == null) {
                Lease newLease = new Lease(
                        selectedTenant.tenant.getId(), selectedUnit.unit.getId(),
                        startDate, endDate, monthlyRent, (String) statusCombo.getSelectedItem());
                leaseDAO.createLease(newLease);
            } else {
                Lease updated = new Lease(
                        editingId, selectedTenant.tenant.getId(), selectedUnit.unit.getId(),
                        startDate, endDate, monthlyRent, (String) statusCombo.getSelectedItem());
                boolean ok = leaseDAO.updateLease(updated);
                if (!ok) {
                    setStatus("This lease no longer exists — it may have been deleted elsewhere.");
                }
            }
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not save lease: " + rootMessage(e));
        }
    }

    private void onDelete() {
        if (editingId == null) {
            setStatus("Select a lease from the list first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this lease? Its payment history will also be affected.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            leaseDAO.deleteLease(editingId);
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not delete lease: " + rootMessage(e));
        }
    }

    private void reload() {
        try {
            List<Lease> all = leaseDAO.getAllLeases();
            tableModel.setRows(all);
        } catch (RuntimeException e) {
            setStatus("Could not load leases: " + rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : t.getMessage();
    }

    private static class TenantOption {
        final Tenant tenant;

        TenantOption(Tenant tenant) {
            this.tenant = tenant;
        }

        @Override
        public String toString() {
            return tenant.getFirstName() + " " + tenant.getLastName();
        }
    }

    private static class UnitOption {
        final Unit unit;

        UnitOption(Unit unit) {
            this.unit = unit;
        }

        @Override
        public String toString() {
            return "Unit " + unit.getUnitNumber();
        }
    }

    private class LeaseTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Tenant", "Unit", "Start", "End", "Rent", "Status"};
        private List<Lease> rows = List.of();
        private final Map<Integer, String> tenantLabelCache = new HashMap<>();
        private final Map<Integer, String> unitLabelCache = new HashMap<>();

        void setRows(List<Lease> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        void clearLabelCaches() {
            tenantLabelCache.clear();
            unitLabelCache.clear();
        }

        Lease getRowAt(int row) {
            return rows.get(row);
        }

        private String tenantLabel(int tenantId) {
            return tenantLabelCache.computeIfAbsent(tenantId, id -> {
                for (int i = 0; i < tenantCombo.getItemCount(); i++) {
                    TenantOption option = tenantCombo.getItemAt(i);
                    if (option.tenant.getId() == id) {
                        return option.toString();
                    }
                }
                return "Tenant id " + id;
            });
        }

        private String unitLabel(int unitId) {
            return unitLabelCache.computeIfAbsent(unitId, id -> {
                for (int i = 0; i < unitCombo.getItemCount(); i++) {
                    UnitOption option = unitCombo.getItemAt(i);
                    if (option.unit.getId() == id) {
                        return option.toString();
                    }
                }
                return "Unit id " + id;
            });
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Lease l = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return l.getId();
                case 1: return tenantLabel(l.getTenantId());
                case 2: return unitLabel(l.getUnitId());
                case 3: return l.getStartDate();
                case 4: return l.getEndDate();
                case 5: return l.getMonthlyRent();
                case 6: return l.getStatus();
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}