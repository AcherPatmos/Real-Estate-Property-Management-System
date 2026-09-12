package com.Propertmanagement.gui;

import com.Propertmanagement.dao.BuildingDAO;
import com.Propertmanagement.dao.FloorDAO;
import com.Propertmanagement.dao.PropertyDAO;
import com.Propertmanagement.dao.UnitDAO;
import com.Propertmanagement.model.Building;
import com.Propertmanagement.model.Floor;
import com.Propertmanagement.model.Property;
import com.Propertmanagement.model.Unit;
import com.Propertmanagement.validation.ParsedField;
import com.Propertmanagement.validation.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// CRUD screen for Unit.

public class UnitPanel extends JPanel {

    private static final int UNIT_NUMBER_MAX_LENGTH = 50;
    private static final String[] STATUS_OPTIONS = {"VACANT", "OCCUPIED", "MAINTENANCE"};

    private final UnitDAO unitDAO;
    // Needed only to walk floor -> building -> property
    private final FloorDAO floorDAO;
    private final BuildingDAO buildingDAO;
    private final PropertyDAO propertyDAO;

    // Combo holds FloorOption wrappers
    private final JComboBox<FloorOption> floorCombo = new JComboBox<>();
    private final JTextField unitNumberField = new JTextField();
    private final JComboBox<String> statusCombo = new JComboBox<>(STATUS_OPTIONS);
    private final JTextField rentField = new JTextField();

    private final JLabel floorError = errorLabel();
    private final JLabel unitNumberError = errorLabel();
    private final JLabel rentError = errorLabel();
    private final JLabel statusLabel = new JLabel(" ");

    private final UnitTableModel tableModel;
    private final JTable table;

    private final JButton saveButton = new JButton("Save");
    private final JButton newButton = new JButton("New");
    private final JButton deleteButton = new JButton("Delete");
    // Manual re-fetch of the floor dropdown, since floors can change via a different screen/session and this one won't auto-notice
    private final JButton refreshFloorsButton = new JButton("Refresh floors");

    private Integer editingId = null;

    public UnitPanel(UnitDAO unitDAO, FloorDAO floorDAO, BuildingDAO buildingDAO, PropertyDAO propertyDAO) {
        super(new BorderLayout(12, 12));
        this.unitDAO = unitDAO;
        this.floorDAO = floorDAO;
        this.buildingDAO = buildingDAO;
        this.propertyDAO = propertyDAO;
        this.tableModel = new UnitTableModel();
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
        refreshFloorsButton.addActionListener(e -> reloadFloors());

        reloadFloors(); // dropdown must be populated before the table, since row selection depends on matching floor IDs to options
        reload();
    }

    // Lays out Floor dropdown, Unit number, Status dropdown, Rent field, and buttons in a grid
    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Unit");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        form.add(heading, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Floor:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(floorCombo, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        form.add(refreshFloorsButton, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(floorError, gbc);
        gbc.gridwidth = 1;

        addFieldRow(form, gbc, 3, "Unit number", unitNumberField, unitNumberError);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        form.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(statusCombo, gbc);
        gbc.gridwidth = 1;

        addFieldRow(form, gbc, 6, "Rent amount", rentField, rentError);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(saveButton);
        buttons.add(newButton);
        buttons.add(deleteButton);
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 3;
        form.add(buttons, gbc);

        gbc.gridy = 9;
        statusLabel.setForeground(new Color(178, 34, 34));
        form.add(statusLabel, gbc);

        return form;
    }

    // Same label + field + error-label row layout used across every panel in this package
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

    // Re-fetches every Floor and rebuilds the dropdown with "Property › Building › Floor N" labels, restoring the prior selection if possible
    private void reloadFloors() {
        FloorOption previouslySelected = (FloorOption) floorCombo.getSelectedItem();
        floorCombo.removeAllItems();
        tableModel.clearFloorLabelCache(); // stale cached labels would otherwise survive a floor rename/move

        try {
            List<Floor> floors = floorDAO.getAllFloors();
            // Caches avoid re-querying the same Building/Property multiple times when many floors share one building
            Map<Integer, Building> buildingCache = new HashMap<>();
            Map<Integer, Property> propertyCache = new HashMap<>();

            for (Floor floor : floors) {
                Building building = buildingCache.computeIfAbsent(floor.getBuildingId(), buildingDAO::getBuildingById);
                String label;
                if (building == null) {
                    // shows a broken FK instead of crashing or silently hiding the floor
                    label = "Floor " + floor.getFloorNumber() + " (building missing, id " + floor.getBuildingId() + ")";
                } else {
                    Property property = propertyCache.computeIfAbsent(building.getPropertyId(), propertyDAO::getPropertyById);
                    String propertyName = property == null ? "Unknown property" : property.getName();
                    label = propertyName + " › " + building.getName() + " › Floor " + floor.getFloorNumber();
                }
                floorCombo.addItem(new FloorOption(floor, label));
            }

            setStatus(floors.isEmpty()
                    ? "No floors exist yet — add a building and floor (directly, until those screens exist) before creating units."
                    : null);

            // Re-select the floor that was chosen before the refresh, matched by ID since the FloorOption instance itself is now different
            if (previouslySelected != null) {
                for (int i = 0; i < floorCombo.getItemCount(); i++) {
                    if (floorCombo.getItemAt(i).floor.getId() == previouslySelected.floor.getId()) {
                        floorCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            setStatus("Could not load floors: " + rootMessage(e));
        }
    }

    // Loads the selected unit's fields into the form, including matching its floor_id to a dropdown entry
    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        Unit selected = tableModel.getRowAt(table.convertRowIndexToModel(row));
        editingId = selected.getId();
        selectFloorById(selected.getFloorId());
        unitNumberField.setText(selected.getUnitNumber());
        statusCombo.setSelectedItem(selected.getStatus());
        rentField.setText(selected.getRentAmount() == null ? "" : selected.getRentAmount().toPlainString());
        clearErrors();
        setStatus(null);
    }

    private void selectFloorById(int floorId) {
        for (int i = 0; i < floorCombo.getItemCount(); i++) {
            if (floorCombo.getItemAt(i).floor.getId() == floorId) {
                floorCombo.setSelectedIndex(i);
                return;
            }
        }

    }

    private void clearForm() {
        editingId = null;
        if (floorCombo.getItemCount() > 0) {
            floorCombo.setSelectedIndex(0);
        }
        unitNumberField.setText("");
        statusCombo.setSelectedIndex(0);
        rentField.setText("");
        table.clearSelection();
        clearErrors();
        setStatus(null);
    }

    private void clearErrors() {
        floorError.setText(" ");
        unitNumberError.setText(" ");
        rentError.setText(" ");
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null ? " " : message);
    }

    // Validates floor selection + unit number + rent, re-verifies the floor still exists, then creates or updates the Unit
    private void onSave() {
        clearErrors();
        boolean valid = true;

        FloorOption selectedFloor = (FloorOption) floorCombo.getSelectedItem();
        if (selectedFloor == null) {
            floorError.setText("Choose a floor — add a building and floor first if none are listed.");
            valid = false;
        }

        ParsedField<String> unitNumberResult =
                ValidationUtils.requireText(unitNumberField.getText(), "Unit number", UNIT_NUMBER_MAX_LENGTH);
        if (!unitNumberResult.isValid()) {
            unitNumberError.setText(unitNumberResult.getError());
            valid = false;
        }

        // requireMoney parses/validates rent as BigDecimal rather than double, avoiding floating-point rounding on currency
        ParsedField<BigDecimal> rentResult = ValidationUtils.requireMoney(rentField.getText(), "Rent amount");
        if (!rentResult.isValid()) {
            rentError.setText(rentResult.getError());
            valid = false;
        }

        if (!valid) {
            return;
        }

        String status = (String) statusCombo.getSelectedItem();

        try {
            // Guard against the floor having been deleted since the dropdown was populated.
            if (floorDAO.getFloorById(selectedFloor.floor.getId()) == null) {
                floorError.setText("This floor no longer exists. Refreshing the floor list.");
                reloadFloors();
                return;
            }

            if (editingId == null) {
                Unit newUnit = new Unit(selectedFloor.floor.getId(), unitNumberResult.getValue(), status, rentResult.getValue());
                unitDAO.createUnit(newUnit);
            } else {
                Unit updated = new Unit(editingId, selectedFloor.floor.getId(), unitNumberResult.getValue(), status, rentResult.getValue());
                boolean ok = unitDAO.updateUnit(updated);
                if (!ok) {
                    setStatus("This unit no longer exists — it may have been deleted elsewhere.");
                }
            }
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not save unit: " + rootMessage(e));
        }
    }

    // Confirms, then deletes the unit being edited
    private void onDelete() {
        if (editingId == null) {
            setStatus("Select a unit from the list first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this unit?",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            unitDAO.deleteUnit(editingId);
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not delete unit: " + rootMessage(e));
        }
    }

    private void reload() {
        try {
            List<Unit> all = unitDAO.getAllUnits();
            tableModel.setRows(all);
        } catch (RuntimeException e) {
            setStatus("Could not load units: " + rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : t.getMessage();
    }

    // Wraps a Floor with its precomputed context label so the combo box displays "Property › Building › Floor N" instead of a bare Floor object
    private static class FloorOption {
        final Floor floor;
        final String label;

        FloorOption(Floor floor, String label) {
            this.floor = floor;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // Table model for the Unit list; resolves each row's floor_id to a readable label via the same lookup logic as the dropdown
    private class UnitTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Floor", "Unit #", "Status", "Rent"};
        private List<Unit> rows = List.of();
        // Caches floor_id
        private final Map<Integer, String> floorLabelCache = new HashMap<>();

        void setRows(List<Unit> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        void clearFloorLabelCache() {
            floorLabelCache.clear();
        }

        Unit getRowAt(int row) {
            return rows.get(row);
        }

        // Looks up a floor's label from the currently loaded combo box options, falling back to a bare ID if not found
        private String floorLabel(int floorId) {
            return floorLabelCache.computeIfAbsent(floorId, id -> {
                for (int i = 0; i < floorCombo.getItemCount(); i++) {
                    FloorOption option = floorCombo.getItemAt(i);
                    if (option.floor.getId() == id) {
                        return option.label;
                    }
                }
                return "Floor id " + id;
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
            Unit u = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return u.getId();
                case 1: return floorLabel(u.getFloorId());
                case 2: return u.getUnitNumber();
                case 3: return u.getStatus();
                case 4: return u.getRentAmount();
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}