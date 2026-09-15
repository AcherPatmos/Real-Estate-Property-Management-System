package com.Propertmanagement.gui;

import com.Propertmanagement.dao.ExpenseDAO;
import com.Propertmanagement.dao.PropertyDAO;
import com.Propertmanagement.dao.UnitDAO;
import com.Propertmanagement.model.Expense;
import com.Propertmanagement.model.Property;
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

// CRUD screen for Expense. A radio-button pair picks whether this expense
// belongs to a Property or a Unit; only the matching dropdown is enabled,
// mirroring the "exactly one owner" rule ExpenseDAO enforces on save.
public class ExpensesPanel extends JPanel {

    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private final ExpenseDAO expenseDAO;
    private final PropertyDAO propertyDAO;
    private final UnitDAO unitDAO;

    private final JRadioButton propertyRadio = new JRadioButton("Property");
    private final JRadioButton unitRadio = new JRadioButton("Unit");
    private final JComboBox<PropertyOption> propertyCombo = new JComboBox<>();
    private final JComboBox<UnitOption> unitCombo = new JComboBox<>();
    private final JTextField descriptionField = new JTextField();
    private final JTextField amountField = new JTextField();
    private final JTextField expenseDateField = new JTextField();

    private final JLabel ownerError = errorLabel();
    private final JLabel descriptionError = errorLabel();
    private final JLabel amountError = errorLabel();
    private final JLabel expenseDateError = errorLabel();
    private final JLabel statusLabel = new JLabel(" ");

    private final ExpenseTableModel tableModel;
    private final JTable table;

    private final JButton saveButton = new JButton("Save");
    private final JButton newButton = new JButton("New");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton refreshListsButton = new JButton("Refresh properties/units");

    private Integer editingId = null;

    public ExpensesPanel(ExpenseDAO expenseDAO, PropertyDAO propertyDAO, UnitDAO unitDAO) {
        super(new BorderLayout(12, 12));
        this.expenseDAO = expenseDAO;
        this.propertyDAO = propertyDAO;
        this.unitDAO = unitDAO;
        this.tableModel = new ExpenseTableModel();
        this.table = new JTable(tableModel);

        ButtonGroup ownerGroup = new ButtonGroup();
        ownerGroup.add(propertyRadio);
        ownerGroup.add(unitRadio);
        propertyRadio.setSelected(true);
        propertyRadio.addActionListener(e -> updateOwnerEnablement());
        unitRadio.addActionListener(e -> updateOwnerEnablement());

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

        updateOwnerEnablement();
        reloadDropdowns();
        reload();
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Expense");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        form.add(heading, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Attach to:"), gbc);
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        radios.add(propertyRadio);
        radios.add(unitRadio);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(radios, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        form.add(new JLabel("Property:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(propertyCombo, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        form.add(refreshListsButton, gbc);

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
        gbc.weightx = 1;
        form.add(ownerError, gbc);
        gbc.gridwidth = 1;

        addFieldRow(form, gbc, 5, "Description", descriptionField, descriptionError);
        addFieldRow(form, gbc, 7, "Amount", amountField, amountError);
        addFieldRow(form, gbc, 9, "Expense Date (yyyy-MM-dd)", expenseDateField, expenseDateError);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(saveButton);
        buttons.add(newButton);
        buttons.add(deleteButton);
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.gridwidth = 3;
        form.add(buttons, gbc);

        gbc.gridy = 12;
        statusLabel.setForeground(new Color(178, 34, 34));
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

    // Enables exactly one dropdown based on which radio is selected - the UI-level
    // mirror of ExpenseDAO's validateExactlyOneOwner check.
    private void updateOwnerEnablement() {
        propertyCombo.setEnabled(propertyRadio.isSelected());
        unitCombo.setEnabled(unitRadio.isSelected());
    }

    private void reloadDropdowns() {
        PropertyOption previousProperty = (PropertyOption) propertyCombo.getSelectedItem();
        UnitOption previousUnit = (UnitOption) unitCombo.getSelectedItem();

        propertyCombo.removeAllItems();
        unitCombo.removeAllItems();
        tableModel.clearLabelCaches();

        try {
            List<Property> properties = propertyDAO.getAllProperties();
            for (Property p : properties) {
                propertyCombo.addItem(new PropertyOption(p));
            }
            List<Unit> units = unitDAO.getAllUnits();
            for (Unit u : units) {
                unitCombo.addItem(new UnitOption(u));
            }
            setStatus((properties.isEmpty() && units.isEmpty())
                    ? "Add at least one property or unit before recording an expense." : null);

            if (previousProperty != null) {
                for (int i = 0; i < propertyCombo.getItemCount(); i++) {
                    if (propertyCombo.getItemAt(i).property.getId() == previousProperty.property.getId()) {
                        propertyCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (previousUnit != null) {
                for (int i = 0; i < unitCombo.getItemCount(); i++) {
                    if (unitCombo.getItemAt(i).unit.getId() == previousUnit.unit.getId()) {
                        unitCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            setStatus("Could not load properties/units: " + rootMessage(e));
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        Expense selected = tableModel.getRowAt(table.convertRowIndexToModel(row));
        editingId = selected.getId();

        if (selected.isPropertyLevel()) {
            propertyRadio.setSelected(true);
            selectPropertyById(selected.getPropertyId());
        } else {
            unitRadio.setSelected(true);
            selectUnitById(selected.getUnitId());
        }
        updateOwnerEnablement();

        descriptionField.setText(selected.getDescription());
        amountField.setText(selected.getAmount().toPlainString());
        expenseDateField.setText(selected.getExpenseDate().toString());
        clearErrors();
        setStatus(null);
    }

    private void selectPropertyById(int propertyId) {
        for (int i = 0; i < propertyCombo.getItemCount(); i++) {
            if (propertyCombo.getItemAt(i).property.getId() == propertyId) {
                propertyCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectUnitById(int unitId) {
        for (int i = 0; i < unitCombo.getItemCount(); i++) {
            if (unitCombo.getItemAt(i).unit.getId() == unitId) {
                unitCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void clearForm() {
        editingId = null;
        propertyRadio.setSelected(true);
        updateOwnerEnablement();
        if (propertyCombo.getItemCount() > 0) {
            propertyCombo.setSelectedIndex(0);
        }
        if (unitCombo.getItemCount() > 0) {
            unitCombo.setSelectedIndex(0);
        }
        descriptionField.setText("");
        amountField.setText("");
        expenseDateField.setText("");
        table.clearSelection();
        clearErrors();
        setStatus(null);
    }

    private void clearErrors() {
        ownerError.setText(" ");
        descriptionError.setText(" ");
        amountError.setText(" ");
        expenseDateError.setText(" ");
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null ? " " : message);
    }

    private void onSave() {
        clearErrors();
        boolean valid = true;

        Integer propertyId = null;
        Integer unitId = null;

        if (propertyRadio.isSelected()) {
            PropertyOption selected = (PropertyOption) propertyCombo.getSelectedItem();
            if (selected == null) {
                ownerError.setText("Choose a property — add one first if none are listed.");
                valid = false;
            } else {
                propertyId = selected.property.getId();
            }
        } else {
            UnitOption selected = (UnitOption) unitCombo.getSelectedItem();
            if (selected == null) {
                ownerError.setText("Choose a unit — add one first if none are listed.");
                valid = false;
            } else {
                unitId = selected.unit.getId();
            }
        }

        String description = descriptionField.getText().trim();
        if (description.isEmpty()) {
            descriptionError.setText("Description is required.");
            valid = false;
        } else if (description.length() > DESCRIPTION_MAX_LENGTH) {
            descriptionError.setText("Description must be " + DESCRIPTION_MAX_LENGTH + " characters or fewer.");
            valid = false;
        }

        BigDecimal amount = null;
        try {
            amount = new BigDecimal(amountField.getText().trim());
            if (amount.signum() <= 0) {
                amountError.setText("Amount must be greater than zero.");
                valid = false;
            }
        } catch (NumberFormatException e) {
            amountError.setText("Enter a valid amount, e.g. 120.00");
            valid = false;
        }

        LocalDate expenseDate = null;
        try {
            expenseDate = LocalDate.parse(expenseDateField.getText().trim());
        } catch (DateTimeParseException e) {
            expenseDateError.setText("Enter a valid date as yyyy-MM-dd.");
            valid = false;
        }

        if (!valid) {
            return;
        }

        try {
            if (editingId == null) {
                Expense newExpense = new Expense(propertyId, unitId, description, amount, expenseDate);
                expenseDAO.createExpense(newExpense);
            } else {
                Expense updated = new Expense(editingId, propertyId, unitId, description, amount, expenseDate);
                boolean ok = expenseDAO.updateExpense(updated);
                if (!ok) {
                    setStatus("This expense no longer exists — it may have been deleted elsewhere.");
                }
            }
            clearForm();
            reload();
        } catch (IllegalArgumentException e) {
            // From ExpenseDAO.validateExactlyOneOwner - shouldn't normally trigger since the
            // radio buttons already prevent it, but kept as a safety net.
            ownerError.setText(e.getMessage());
        } catch (RuntimeException e) {
            setStatus("Could not save expense: " + rootMessage(e));
        }
    }

    private void onDelete() {
        if (editingId == null) {
            setStatus("Select an expense from the list first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this expense?",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            expenseDAO.deleteExpense(editingId);
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not delete expense: " + rootMessage(e));
        }
    }

    private void reload() {
        try {
            List<Expense> all = expenseDAO.getAllExpenses();
            tableModel.setRows(all);
        } catch (RuntimeException e) {
            setStatus("Could not load expenses: " + rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : t.getMessage();
    }

    private static class PropertyOption {
        final Property property;

        PropertyOption(Property property) {
            this.property = property;
        }

        @Override
        public String toString() {
            return property.getName();
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

    private class ExpenseTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Owner", "Description", "Amount", "Date"};
        private List<Expense> rows = List.of();
        private final Map<Integer, String> propertyLabelCache = new HashMap<>();
        private final Map<Integer, String> unitLabelCache = new HashMap<>();

        void setRows(List<Expense> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        void clearLabelCaches() {
            propertyLabelCache.clear();
            unitLabelCache.clear();
        }

        Expense getRowAt(int row) {
            return rows.get(row);
        }

        private String ownerLabel(Expense expense) {
            if (expense.isPropertyLevel()) {
                int id = expense.getPropertyId();
                return propertyLabelCache.computeIfAbsent(id, pid -> {
                    for (int i = 0; i < propertyCombo.getItemCount(); i++) {
                        PropertyOption option = propertyCombo.getItemAt(i);
                        if (option.property.getId() == pid) {
                            return "Property: " + option.property.getName();
                        }
                    }
                    return "Property id " + pid;
                });
            } else {
                int id = expense.getUnitId();
                return unitLabelCache.computeIfAbsent(id, uid -> {
                    for (int i = 0; i < unitCombo.getItemCount(); i++) {
                        UnitOption option = unitCombo.getItemAt(i);
                        if (option.unit.getId() == uid) {
                            return "Unit: " + option.unit.getUnitNumber();
                        }
                    }
                    return "Unit id " + uid;
                });
            }
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
            Expense e = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return e.getId();
                case 1: return ownerLabel(e);
                case 2: return e.getDescription();
                case 3: return e.getAmount();
                case 4: return e.getExpenseDate();
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}