package com.Propertmanagement.gui;

import com.Propertmanagement.dao.BuildingDAO;
import com.Propertmanagement.dao.FloorDAO;
import com.Propertmanagement.dao.PropertyDAO;
import com.Propertmanagement.model.Building;
import com.Propertmanagement.model.Floor;
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

// CRUD screen for Floor.
public class FloorPanel extends JPanel {

    private final FloorDAO floorDAO;
    private final BuildingDAO buildingDAO;
    // Needed only to resolve a building's property name for the dropdown label
    private final PropertyDAO propertyDAO;

    // Combo holds BuildingOption wrappers (building + "Property › Building" label), not raw Building objects
    private final JComboBox<BuildingOption> buildingCombo = new JComboBox<>();
    private final JTextField floorNumberField = new JTextField();

    private final JLabel buildingError = errorLabel();
    private final JLabel floorNumberError = errorLabel();
    private final JLabel statusLabel = new JLabel(" ");

    private final FloorTableModel tableModel;
    private final JTable table;

    private final JButton saveButton = new JButton("Save");
    private final JButton newButton = new JButton("New");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton refreshBuildingsButton = new JButton("Refresh buildings");

    private Integer editingId = null;

    public FloorPanel(FloorDAO floorDAO, BuildingDAO buildingDAO, PropertyDAO propertyDAO) {
        super(new BorderLayout(12, 12));
        this.floorDAO = floorDAO;
        this.buildingDAO = buildingDAO;
        this.propertyDAO = propertyDAO;
        this.tableModel = new FloorTableModel();
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
        refreshBuildingsButton.addActionListener(e -> reloadBuildings());

        reloadBuildings(); // populate FK dropdown before the table
        reload();
    }

    // Lays out Building dropdown, Floor number field, and buttons
    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Floor");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        form.add(heading, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Building:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(buildingCombo, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        form.add(refreshBuildingsButton, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(buildingError, gbc);
        gbc.gridwidth = 1;

        addFieldRow(form, gbc, 3, "Floor number", floorNumberField, floorNumberError);

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
        field.setColumns(20);
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

    // Re-fetches all Buildings, resolving each one's Property to build a "Property › Building" label, restoring the prior selection if possible
    private void reloadBuildings() {
        BuildingOption previouslySelected = (BuildingOption) buildingCombo.getSelectedItem();
        buildingCombo.removeAllItems();
        tableModel.clearBuildingLabelCache();
        try {
            List<Building> buildings = buildingDAO.getAllBuildings();
            Map<Integer, Property> propertyCache = new HashMap<>(); // avoids refetching the same Property for multiple buildings

            for (Building building : buildings) {
                Property property = propertyCache.computeIfAbsent(building.getPropertyId(), propertyDAO::getPropertyById);
                String propertyName = property == null ? "Unknown property" : property.getName();
                String label = propertyName + " › " + building.getName();
                buildingCombo.addItem(new BuildingOption(building, label));
            }

            setStatus(buildings.isEmpty() ? "No buildings exist yet — add a building first." : null);

            if (previouslySelected != null) {
                for (int i = 0; i < buildingCombo.getItemCount(); i++) {
                    if (buildingCombo.getItemAt(i).building.getId() == previouslySelected.building.getId()) {
                        buildingCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            setStatus("Could not load buildings: " + rootMessage(e));
        }
    }

    // Loads the selected floor into the form, matching its building_id to a dropdown entry
    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        Floor selected = tableModel.getRowAt(table.convertRowIndexToModel(row));
        editingId = selected.getId();
        selectBuildingById(selected.getBuildingId());
        floorNumberField.setText(String.valueOf(selected.getFloorNumber()));
        clearErrors();
        setStatus(null);
    }

    private void selectBuildingById(int buildingId) {
        for (int i = 0; i < buildingCombo.getItemCount(); i++) {
            if (buildingCombo.getItemAt(i).building.getId() == buildingId) {
                buildingCombo.setSelectedIndex(i);
                return;
            }
        }
        // This floor's building isn't in the current dropdown (e.g. deleted
        // since it was loaded). Leave selection as-is; onSave() re-checks before saving.
    }

    private void clearForm() {
        editingId = null;
        if (buildingCombo.getItemCount() > 0) {
            buildingCombo.setSelectedIndex(0);
        }
        floorNumberField.setText("");
        table.clearSelection();
        clearErrors();
        setStatus(null);
    }

    private void clearErrors() {
        buildingError.setText(" ");
        floorNumberError.setText(" ");
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null ? " " : message);
    }

    // Validates building selection + floor number (as a plain signed int, per the class doc comment), re-verifies the building exists, then saves
    private void onSave() {
        clearErrors();
        boolean valid = true;

        BuildingOption selectedBuilding = (BuildingOption) buildingCombo.getSelectedItem();
        if (selectedBuilding == null) {
            buildingError.setText("Choose a building — add one first if none are listed.");
            valid = false;
        }

        // requireInt, not a "positive only" check — negative (basement) and zero (ground floor) are valid floor numbers
        ParsedField<Integer> floorNumberResult = ValidationUtils.requireInt(floorNumberField.getText(), "Floor number");
        if (!floorNumberResult.isValid()) {
            floorNumberError.setText(floorNumberResult.getError());
            valid = false;
        }

        if (!valid) {
            return;
        }

        try {
            // Guard against the building having been deleted since the dropdown was populated
            if (buildingDAO.getBuildingById(selectedBuilding.building.getId()) == null) {
                buildingError.setText("This building no longer exists. Refreshing the building list.");
                reloadBuildings();
                return;
            }

            if (editingId == null) {
                Floor newFloor = new Floor(selectedBuilding.building.getId(), floorNumberResult.getValue());
                floorDAO.createFloor(newFloor);
            } else {
                Floor updated = new Floor(editingId, selectedBuilding.building.getId(), floorNumberResult.getValue());
                boolean ok = floorDAO.updateFloor(updated);
                if (!ok) {
                    setStatus("This floor no longer exists — it may have been deleted elsewhere.");
                }
            }
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not save floor: " + rootMessage(e));
        }
    }

    // Confirms (mentioning the unit cascade), then deletes the floor being edited
    private void onDelete() {
        if (editingId == null) {
            setStatus("Select a floor from the list first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this floor? This also deletes every unit on it.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            floorDAO.deleteFloor(editingId);
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not delete floor: " + rootMessage(e));
        }
    }

    private void reload() {
        try {
            List<Floor> all = floorDAO.getAllFloors();
            tableModel.setRows(all);
        } catch (RuntimeException e) {
            setStatus("Could not load floors: " + rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : t.getMessage();
    }

    // Wraps a Building with its precomputed "Property › Building" label for combo box display
    private static class BuildingOption {
        final Building building;
        final String label;

        BuildingOption(Building building, String label) {
            this.building = building;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // Table model for the Floor list; resolves each row's building_id to its label via the same cached-lookup pattern as the other panels
    private class FloorTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Building", "Floor #"};
        private List<Floor> rows = List.of();
        private final Map<Integer, String> buildingLabelCache = new HashMap<>();

        void setRows(List<Floor> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        void clearBuildingLabelCache() {
            buildingLabelCache.clear();
        }

        Floor getRowAt(int row) {
            return rows.get(row);
        }

        private String buildingLabel(int buildingId) {
            return buildingLabelCache.computeIfAbsent(buildingId, id -> {
                for (int i = 0; i < buildingCombo.getItemCount(); i++) {
                    BuildingOption option = buildingCombo.getItemAt(i);
                    if (option.building.getId() == id) {
                        return option.label;
                    }
                }
                return "Building id " + id;
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
            Floor f = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return f.getId();
                case 1: return buildingLabel(f.getBuildingId());
                case 2: return f.getFloorNumber();
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}