package com.Propertmanagement.gui;

import com.Propertmanagement.dao.BuildingDAO;
import com.Propertmanagement.dao.PropertyDAO;
import com.Propertmanagement.model.Building;
import com.Propertmanagement.model.Property;
import com.Propertmanagement.validation.ParsedField;
import com.Propertmanagement.validation.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// CRUD screen for Building.
public class BuildingPanel extends JPanel {

    private static final int NAME_MAX_LENGTH = 255;

    private final BuildingDAO buildingDAO;
    private final PropertyDAO propertyDAO;

    // Combo holds PropertyOption wrappers so the dropdown displays property names
    private final JComboBox<PropertyOption> propertyCombo = new JComboBox<>();
    private final JTextField nameField = new JTextField();

    private final JLabel propertyError = errorLabel();
    private final JLabel nameError = errorLabel();
    private final JLabel statusLabel = new JLabel(" ");

    private final BuildingTableModel tableModel;
    private final JTable table;

    private final JButton saveButton = new JButton("Save");
    private final JButton newButton = new JButton("New");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton refreshPropertiesButton = new JButton("Refresh properties");

    private Integer editingId = null;

    public BuildingPanel(BuildingDAO buildingDAO, PropertyDAO propertyDAO) {
        super(new BorderLayout(12, 12));
        this.buildingDAO = buildingDAO;
        this.propertyDAO = propertyDAO;
        this.tableModel = new BuildingTableModel();
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
        refreshPropertiesButton.addActionListener(e -> reloadProperties());

        reloadProperties(); // populate FK dropdown before the table, same ordering reason as UnitPanel
        reload();
    }

    // Lays out Property dropdown, Name field, and buttons
    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Building");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        form.add(heading, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Property:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(propertyCombo, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        form.add(refreshPropertiesButton, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(propertyError, gbc);
        gbc.gridwidth = 1;

        addFieldRow(form, gbc, 3, "Name", nameField, nameError);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(saveButton);
        buttons.add(newButton);
        buttons.add(deleteButton);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        form.add(buttons, gbc);

        gbc.gridy = 6;
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

    // Re-fetches all Properties and rebuilds the dropdown, restoring the prior selection by ID if it still exists
    private void reloadProperties() {
        PropertyOption previouslySelected = (PropertyOption) propertyCombo.getSelectedItem();
        propertyCombo.removeAllItems();
        tableModel.clearPropertyLabelCache();
        try {
            List<Property> properties = propertyDAO.getAllProperties();
            for (Property property : properties) {
                propertyCombo.addItem(new PropertyOption(property));
            }
            setStatus(properties.isEmpty() ? "No properties exist yet — add a property first." : null);

            if (previouslySelected != null) {
                for (int i = 0; i < propertyCombo.getItemCount(); i++) {
                    if (propertyCombo.getItemAt(i).property.getId() == previouslySelected.property.getId()) {
                        propertyCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            setStatus("Could not load properties: " + rootMessage(e));
        }
    }

    // Loads the selected building into the form, matching its property_id to a dropdown entry
    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        Building selected = tableModel.getRowAt(table.convertRowIndexToModel(row));
        editingId = selected.getId();
        selectPropertyById(selected.getPropertyId());
        nameField.setText(selected.getName());
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

    private void clearForm() {
        editingId = null;
        if (propertyCombo.getItemCount() > 0) {
            propertyCombo.setSelectedIndex(0);
        }
        nameField.setText("");
        table.clearSelection();
        clearErrors();
        setStatus(null);
    }

    private void clearErrors() {
        propertyError.setText(" ");
        nameError.setText(" ");
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null ? " " : message);
    }

    // Validates property selection + name, re-verifies the property still exists, then creates or updates the Building
    private void onSave() {
        clearErrors();
        boolean valid = true;

        PropertyOption selectedProperty = (PropertyOption) propertyCombo.getSelectedItem();
        if (selectedProperty == null) {
            propertyError.setText("Choose a property — add one first if none are listed.");
            valid = false;
        }

        ParsedField<String> nameResult = ValidationUtils.requireText(nameField.getText(), "Name", NAME_MAX_LENGTH);
        if (!nameResult.isValid()) {
            nameError.setText(nameResult.getError());
            valid = false;
        }

        if (!valid) {
            return;
        }

        try {
            // Guard against the property having been deleted since the dropdown was populated
            if (propertyDAO.getPropertyById(selectedProperty.property.getId()) == null) {
                propertyError.setText("This property no longer exists. Refreshing the property list.");
                reloadProperties();
                return;
            }

            if (editingId == null) {
                Building newBuilding = new Building(selectedProperty.property.getId(), nameResult.getValue());
                buildingDAO.createBuilding(newBuilding);
            } else {
                Building updated = new Building(editingId, selectedProperty.property.getId(), nameResult.getValue());
                boolean ok = buildingDAO.updateBuilding(updated);
                if (!ok) {
                    setStatus("This building no longer exists — it may have been deleted elsewhere.");
                }
            }
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not save building: " + rootMessage(e));
        }
    }

    // Confirms (mentioning the floor/unit cascade), then deletes the building being edited
    private void onDelete() {
        if (editingId == null) {
            setStatus("Select a building from the list first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this building? This also deletes every floor and unit beneath it.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            buildingDAO.deleteBuilding(editingId);
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not delete building: " + rootMessage(e));
        }
    }

    private void reload() {
        try {
            List<Building> all = buildingDAO.getAllBuildings();
            tableModel.setRows(all);
        } catch (RuntimeException e) {
            setStatus("Could not load buildings: " + rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : t.getMessage();
    }

    // Wraps a Property so the combo box displays its name via toString() instead of the object's default representation
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

    // Table model for the Building list; resolves each row's property_id to its name via the same lookup pattern as UnitPanel
    private class BuildingTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Property", "Name"};
        private List<Building> rows = List.of();
        private final Map<Integer, String> propertyLabelCache = new HashMap<>();

        void setRows(List<Building> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        void clearPropertyLabelCache() {
            propertyLabelCache.clear();
        }

        Building getRowAt(int row) {
            return rows.get(row);
        }

        private String propertyLabel(int propertyId) {
            return propertyLabelCache.computeIfAbsent(propertyId, id -> {
                for (int i = 0; i < propertyCombo.getItemCount(); i++) {
                    PropertyOption option = propertyCombo.getItemAt(i);
                    if (option.property.getId() == id) {
                        return option.property.getName();
                    }
                }
                return "Property id " + id;
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
            Building b = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return b.getId();
                case 1: return propertyLabel(b.getPropertyId());
                case 2: return b.getName();
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}