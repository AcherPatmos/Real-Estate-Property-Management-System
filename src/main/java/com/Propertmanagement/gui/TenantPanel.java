package com.Propertmanagement.gui;

import com.Propertmanagement.dao.TenantDAO;
import com.Propertmanagement.model.Tenant;
import com.Propertmanagement.validation.ParsedField;
import com.Propertmanagement.validation.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

// CRUD screen for Tenant.
public class TenantPanel extends JPanel {

    private static final int NAME_MAX_LENGTH = 100;
    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int PHONE_MAX_LENGTH = 50;

    private final TenantDAO tenantDAO;

    private final JTextField firstNameField = new JTextField();
    private final JTextField lastNameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();

    private final JLabel firstNameError = errorLabel();
    private final JLabel lastNameError = errorLabel();
    private final JLabel emailError = errorLabel();
    private final JLabel phoneError = errorLabel();
    private final JLabel statusLabel = new JLabel(" ");

    private final TenantTableModel tableModel;
    private final JTable table;

    private final JButton saveButton = new JButton("Save");
    private final JButton newButton = new JButton("New");
    private final JButton deleteButton = new JButton("Delete");

    private Integer editingId = null;

    public TenantPanel(TenantDAO tenantDAO) {
        super(new BorderLayout(12, 12));
        this.tenantDAO = tenantDAO;
        this.tableModel = new TenantTableModel();
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

        reload();
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Tenant");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        form.add(heading, gbc);
        gbc.gridwidth = 1;

        addFieldRow(form, gbc, 1, "First Name", firstNameField, firstNameError);
        addFieldRow(form, gbc, 3, "Last Name", lastNameField, lastNameError);
        addFieldRow(form, gbc, 5, "Email", emailField, emailError);
        addFieldRow(form, gbc, 7, "Phone", phoneField, phoneError);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(saveButton);
        buttons.add(newButton);
        buttons.add(deleteButton);
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 3;
        form.add(buttons, gbc);

        gbc.gridy = 10;
        statusLabel.setForeground(new Color(178, 34, 34));
        form.add(statusLabel, gbc);

        return form;
    }

    // Same two-row-per-field layout as BuildingPanel.addFieldRow: label+field, then an error row beneath it.
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

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        Tenant selected = tableModel.getRowAt(table.convertRowIndexToModel(row));
        editingId = selected.getId();
        firstNameField.setText(selected.getFirstName());
        lastNameField.setText(selected.getLastName());
        emailField.setText(selected.getEmail());
        phoneField.setText(selected.getPhone() == null ? "" : selected.getPhone());
        clearErrors();
        setStatus(null);
    }

    private void clearForm() {
        editingId = null;
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        table.clearSelection();
        clearErrors();
        setStatus(null);
    }

    private void clearErrors() {
        firstNameError.setText(" ");
        lastNameError.setText(" ");
        emailError.setText(" ");
        phoneError.setText(" ");
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null ? " " : message);
    }

    private void onSave() {
        clearErrors();
        boolean valid = true;

        ParsedField<String> firstNameResult =
                ValidationUtils.requireText(firstNameField.getText(), "First name", NAME_MAX_LENGTH);
        if (!firstNameResult.isValid()) {
            firstNameError.setText(firstNameResult.getError());
            valid = false;
        }

        ParsedField<String> lastNameResult =
                ValidationUtils.requireText(lastNameField.getText(), "Last name", NAME_MAX_LENGTH);
        if (!lastNameResult.isValid()) {
            lastNameError.setText(lastNameResult.getError());
            valid = false;
        }

        ParsedField<String> emailResult =
                ValidationUtils.requireText(emailField.getText(), "Email", EMAIL_MAX_LENGTH);
        if (!emailResult.isValid()) {
            emailError.setText(emailResult.getError());
            valid = false;
        }

        // Phone is optional - only length-check it, don't require it (matches TenantDAO/schema allowing NULL).
        String phoneText = phoneField.getText().trim();
        if (phoneText.length() > PHONE_MAX_LENGTH) {
            phoneError.setText("Phone must be " + PHONE_MAX_LENGTH + " characters or fewer.");
            valid = false;
        }

        if (!valid) {
            return;
        }

        String phoneValue = phoneText.isEmpty() ? null : phoneText;

        try {
            if (editingId == null) {
                Tenant newTenant = new Tenant(
                        firstNameResult.getValue(), lastNameResult.getValue(),
                        emailResult.getValue(), phoneValue);
                tenantDAO.createTenant(newTenant);
            } else {
                Tenant updated = new Tenant(
                        editingId, firstNameResult.getValue(), lastNameResult.getValue(),
                        emailResult.getValue(), phoneValue);
                boolean ok = tenantDAO.updateTenant(updated);
                if (!ok) {
                    setStatus("This tenant no longer exists, it may have been deleted elsewhere.");
                }
            }
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not save tenant: " + rootMessage(e));
        }
    }

    private void onDelete() {
        if (editingId == null) {
            setStatus("Select a tenant from the list first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this tenant? Any leases tied to this tenant will be affected.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            tenantDAO.deleteTenant(editingId);
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not delete tenant: " + rootMessage(e));
        }
    }

    private void reload() {
        try {
            List<Tenant> all = tenantDAO.getAllTenants();
            tableModel.setRows(all);
        } catch (RuntimeException e) {
            setStatus("Could not load tenants: " + rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : t.getMessage();
    }

    private class TenantTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "First Name", "Last Name", "Email", "Phone"};
        private List<Tenant> rows = List.of();

        void setRows(List<Tenant> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        Tenant getRowAt(int row) {
            return rows.get(row);
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
            Tenant t = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return t.getId();
                case 1: return t.getFirstName();
                case 2: return t.getLastName();
                case 3: return t.getEmail();
                case 4: return t.getPhone();
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
