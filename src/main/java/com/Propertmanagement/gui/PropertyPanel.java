package com.Propertmanagement.gui;

import com.Propertmanagement.dao.PropertyDAO;
import com.Propertmanagement.model.Property;
import com.Propertmanagement.validation.ParsedField;
import com.Propertmanagement.validation.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

// * CRUD screen for Property
public class PropertyPanel extends JPanel {

    private static final int NAME_MAX_LENGTH = 255;
    private static final int ADDRESS_MAX_LENGTH = 255;

    private final PropertyDAO propertyDAO;

    private final JTextField nameField = new JTextField();
    private final JTextField addressField = new JTextField();
    // Per-field error labels sit right under their inputs, rather than one shared error area
    private final JLabel nameError = errorLabel();
    private final JLabel addressError = errorLabel();
    private final JLabel statusLabel = new JLabel(" ");

    // Backs the JTable
    private final PropertyTableModel tableModel = new PropertyTableModel();
    private final JTable table = new JTable(tableModel);

    private final JButton saveButton = new JButton("Save");
    private final JButton newButton = new JButton("New");
    private final JButton deleteButton = new JButton("Delete");

    private Integer editingId = null; // null => creating a new property; non-null => editing that property's row

    public PropertyPanel(PropertyDAO propertyDAO) {
        super(new BorderLayout(12, 12));
        this.propertyDAO = propertyDAO;
        setBorder(new EmptyBorder(16, 16, 16, 16));

        add(buildForm(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);

        // Clicking a table row loads that property into the form for editing (fires once per click, not per drag-adjust)
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onRowSelected();
            }
        });

        newButton.addActionListener(e -> clearForm());
        saveButton.addActionListener(e -> onSave());
        deleteButton.addActionListener(e -> onDelete());

        reload(); // populate the table on startup
    }

    // Lays out the heading, Name/Address fields with their error labels, and the Save/New/Delete buttons
    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Property");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        form.add(heading, gbc);
        gbc.gridwidth = 1;

        addFieldRow(form, gbc, 1, "Name", nameField, nameError);
        addFieldRow(form, gbc, 3, "Address", addressField, addressError);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(saveButton);
        buttons.add(newButton);
        buttons.add(deleteButton);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        form.add(buttons, gbc);

        gbc.gridy = 6;
        statusLabel.setForeground(new Color(178, 34, 34));
        form.add(statusLabel, gbc);

        return form;
    }

    // Places one label + input + error-label trio into the form grid at the given row
    private void addFieldRow(JPanel form, GridBagConstraints gbc, int row, String labelText,
                             JTextField field, JLabel errorLabel) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        form.add(new JLabel(labelText + ":"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        field.setColumns(24);
        form.add(field, gbc);

        gbc.gridx = 1;
        gbc.gridy = row + 1;
        gbc.weightx = 1;
        form.add(errorLabel, gbc);
    }

    // Small red, small-font label factory used for every inline validation message
    private static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(new Color(178, 34, 34));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        return label;
    }

    // Wraps the table in a scroll pane and enables click-to-sort columns
    private JComponent buildTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        return new JScrollPane(table);
    }

    // Copies the selected row's data into the form fields, switching the screen into edit mode
    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        // convertRowIndexToModel accounts for the row sorter, so sorting the table doesn't break which row gets loaded
        Property selected = tableModel.getRowAt(table.convertRowIndexToModel(row));
        editingId = selected.getId();
        nameField.setText(selected.getName());
        addressField.setText(selected.getAddress());
        clearErrors();
        setStatus(null);
    }

    // Resets the form to a blank "create new" state
    private void clearForm() {
        editingId = null;
        nameField.setText("");
        addressField.setText("");
        table.clearSelection();
        clearErrors();
        setStatus(null);
    }

    private void clearErrors() {
        nameError.setText(" ");
        addressError.setText(" ");
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null ? " " : message);
    }

    // Validates the form, then either creates a new Property or updates the one being edited
    private void onSave() {
        clearErrors();

        // Field-level validation runs before any DB call
        ParsedField<String> nameResult = ValidationUtils.requireText(nameField.getText(), "Name", NAME_MAX_LENGTH);
        ParsedField<String> addressResult = ValidationUtils.requireText(addressField.getText(), "Address", ADDRESS_MAX_LENGTH);

        boolean valid = true;
        if (!nameResult.isValid()) {
            nameError.setText(nameResult.getError());
            valid = false;
        }
        if (!addressResult.isValid()) {
            addressError.setText(addressResult.getError());
            valid = false;
        }
        if (!valid) {
            return; // stop here; nothing hits the DAO until both fields pass validation
        }

        try {
            if (editingId == null) {
                // Create mode: no ID yet, DAO will assign one
                Property newProperty = new Property(nameResult.getValue(), addressResult.getValue());
                propertyDAO.createProperty(newProperty);
            } else {
                // Edit mode: reuse the existing ID so the DAO knows which row to update
                Property updated = new Property(editingId, nameResult.getValue(), addressResult.getValue());
                boolean ok = propertyDAO.updateProperty(updated);
                if (!ok) {
                    // Handles the case where someone else deleted this row between load and save (no exception, just false)
                    setStatus("This property no longer exists — it may have been deleted elsewhere.");
                }
            }
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not save property: " + rootMessage(e));
        }
    }

    // Confirms with the user (since deletion cascades to buildings/floors/units)
    private void onDelete() {
        if (editingId == null) {
            setStatus("Select a property from the list first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this property? This also deletes every building, floor, and unit beneath it.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return; // user backed out; no DAO call
        }
        try {
            propertyDAO.deleteProperty(editingId);
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not delete property: " + rootMessage(e));
        }
    }

    // Re-fetches the full property list and refreshes the table; called after every create/update/delete and on startup
    private void reload() {
        try {
            List<Property> all = propertyDAO.getAllProperties();
            tableModel.setRows(all);
        } catch (RuntimeException e) {
            setStatus("Could not load properties: " + rootMessage(e));
        }
    }

    // Unwraps nested exceptions to surface the most specific underlying error message
    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : t.getMessage();
    }

    // Simple table model that displays a List<Property> as ID/Name/Address rows — no editing directly in the cells
    private static class PropertyTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Name", "Address"};
        private List<Property> rows = List.of();

        void setRows(List<Property> rows) {
            this.rows = rows;
            fireTableDataChanged(); // tells the JTable to repaint with the new data
        }

        Property getRowAt(int row) {
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
            Property p = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return p.getId();
                case 1: return p.getName();
                case 2: return p.getAddress();
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false; // editing happens through the form above, not inline in the table
        }
    }
}