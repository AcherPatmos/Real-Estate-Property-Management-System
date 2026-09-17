package com.Propertmanagement.gui;

import com.Propertmanagement.dao.*;
import com.Propertmanagement.factory.MaintenanceTaskFactory;
import com.Propertmanagement.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

// Combined screen for MaintenanceRequest + MaintenanceTask.
// Left: Requests list/form (same pattern as ExpensePanel's Property/Unit picker).
// Right: the selected request's task tree (JTree), a form to add a Task/Subtask
// under whichever node is selected, and a button to run the recursive
// MaintenanceTaskDAO.computeStats() on that node.
public class MaintenancePanel extends JPanel {

    private static final int TITLE_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    private final Maintenance_RequestDAO requestDAO;
    private final MaintenanceTaskDAO taskDAO;
    private final PropertyDAO propertyDAO;
    private final UnitDAO unitDAO;
    private final MaintenanceTaskFactory taskFactory;

    // ---- Request side (left) ----
    private final JRadioButton propertyRadio = new JRadioButton("Property");
    private final JRadioButton unitRadio = new JRadioButton("Unit");
    private final JComboBox<PropertyOption> propertyCombo = new JComboBox<>();
    private final JComboBox<UnitOption> unitCombo = new JComboBox<>();
    private final JTextField requestTitleField = new JTextField();
    private final JTextField requestDescriptionField = new JTextField();
    private final JComboBox<String> requestStatusCombo =
            new JComboBox<>(new String[]{"OPEN", "IN_PROGRESS", "CLOSED"});
    private final JLabel requestOwnerError = errorLabel();
    private final JLabel requestTitleError = errorLabel();
    private final JLabel requestStatusLabel = new JLabel(" ");
    private final RequestTableModel requestTableModel = new RequestTableModel();
    private final JTable requestTable = new JTable(requestTableModel);
    private final JButton requestSaveButton = new JButton("Save");
    private final JButton requestNewButton = new JButton("New");
    private final JButton requestDeleteButton = new JButton("Delete");
    private Integer editingRequestId = null;

    // ---- Task side (right) ----
    private final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Select a request");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
    private final JTree taskTree = new JTree(treeModel);
    private final JTextField taskTitleField = new JTextField();
    private final JTextField taskCostField = new JTextField();
    private final JLabel taskTitleError = errorLabel();
    private final JLabel taskCostError = errorLabel();
    private final JLabel taskStatusLabel = new JLabel(" ");
    private final JLabel statsLabel = new JLabel(" ");
    private final JButton addTaskButton = new JButton("Add under selection");
    private final JButton deleteTaskButton = new JButton("Delete selected task");
    private final JButton computeStatsButton = new JButton("Compute stats for selection");

    private Maintenance_Request selectedRequest = null;

    public MaintenancePanel(Maintenance_RequestDAO requestDAO, MaintenanceTaskDAO taskDAO,
                             PropertyDAO propertyDAO, UnitDAO unitDAO) {
        super(new BorderLayout(12, 12));
        this.requestDAO = requestDAO;
        this.taskDAO = taskDAO;
        this.propertyDAO = propertyDAO;
        this.unitDAO = unitDAO;
        this.taskFactory = new MaintenanceTaskFactory(taskDAO);

        setBorder(new EmptyBorder(16, 16, 16, 16));

        ButtonGroup ownerGroup = new ButtonGroup();
        ownerGroup.add(propertyRadio);
        ownerGroup.add(unitRadio);
        propertyRadio.setSelected(true);
        propertyRadio.addActionListener(e -> updateOwnerEnablement());
        unitRadio.addActionListener(e -> updateOwnerEnablement());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildRequestSide(), buildTaskSide());
        split.setResizeWeight(0.45);
        add(split, BorderLayout.CENTER);

        requestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onRequestRowSelected();
            }
        });
        taskTree.addTreeSelectionListener(e -> updateTaskButtonsEnablement());

        requestNewButton.addActionListener(e -> clearRequestForm());
        requestSaveButton.addActionListener(e -> onSaveRequest());
        requestDeleteButton.addActionListener(e -> onDeleteRequest());
        addTaskButton.addActionListener(e -> onAddTask());
        deleteTaskButton.addActionListener(e -> onDeleteTask());
        computeStatsButton.addActionListener(e -> onComputeStats());

        updateOwnerEnablement();
        updateTaskButtonsEnablement();
        reloadOwnerDropdowns();
        reloadRequests();
    }

    // Request side

    private JComponent buildRequestSide() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(buildRequestForm(), BorderLayout.NORTH);

        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTable.setAutoCreateRowSorter(true);
        panel.add(new JScrollPane(requestTable), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildRequestForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Maintenance Request");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 15f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
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
        gbc.weightx = 1;
        form.add(radios, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        form.add(new JLabel("Property:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(propertyCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        form.add(new JLabel("Unit:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(unitCombo, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        form.add(requestOwnerError, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        form.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        requestTitleField.setColumns(18);
        form.add(requestTitleField, gbc);
        gbc.gridx = 1;
        gbc.gridy = 6;
        form.add(requestTitleError, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(requestDescriptionField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.weightx = 0;
        form.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(requestStatusCombo, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(requestSaveButton);
        buttons.add(requestNewButton);
        buttons.add(requestDeleteButton);
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        form.add(buttons, gbc);

        gbc.gridy = 10;
        requestStatusLabel.setForeground(new Color(178, 34, 34));
        form.add(requestStatusLabel, gbc);

        return form;
    }

    private void updateOwnerEnablement() {
        propertyCombo.setEnabled(propertyRadio.isSelected());
        unitCombo.setEnabled(unitRadio.isSelected());
    }

    private void reloadOwnerDropdowns() {
        propertyCombo.removeAllItems();
        unitCombo.removeAllItems();
        try {
            for (Property p : propertyDAO.getAllProperties()) {
                propertyCombo.addItem(new PropertyOption(p));
            }
            for (Unit u : unitDAO.getAllUnits()) {
                unitCombo.addItem(new UnitOption(u));
            }
        } catch (RuntimeException e) {
            setRequestStatus("Could not load properties/units: " + rootMessage(e));
        }
    }

    private void reloadRequests() {
        try {
            List<Maintenance_Request> all = requestDAO.getAllRequests();
            requestTableModel.setRows(all);
        } catch (RuntimeException e) {
            setRequestStatus("Could not load maintenance requests: " + rootMessage(e));
        }
    }

    private void onRequestRowSelected() {
        int row = requestTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Maintenance_Request selected = requestTableModel.getRowAt(requestTable.convertRowIndexToModel(row));
        editingRequestId = selected.getId();
        selectedRequest = selected;

        if (selected.getPropertyId() != null) {
            propertyRadio.setSelected(true);
            selectComboById(propertyCombo, selected.getPropertyId());
        } else {
            unitRadio.setSelected(true);
            selectComboById(unitCombo, selected.getUnitId());
        }
        updateOwnerEnablement();
        requestTitleField.setText(selected.getTitle());
        requestDescriptionField.setText(selected.getDescription() == null ? "" : selected.getDescription());
        requestStatusCombo.setSelectedItem(selected.getStatus());
        clearRequestErrors();
        setRequestStatus(null);

        reloadTaskTree();
    }

    private void selectComboById(JComboBox<?> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            int itemId = (item instanceof PropertyOption)
                    ? ((PropertyOption) item).property.getId()
                    : ((UnitOption) item).unit.getId();
            if (itemId == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void clearRequestForm() {
        editingRequestId = null;
        selectedRequest = null;
        propertyRadio.setSelected(true);
        updateOwnerEnablement();
        if (propertyCombo.getItemCount() > 0) {
            propertyCombo.setSelectedIndex(0);
        }
        requestTitleField.setText("");
        requestDescriptionField.setText("");
        requestStatusCombo.setSelectedItem("OPEN");
        requestTable.clearSelection();
        clearRequestErrors();
        setRequestStatus(null);
        resetTaskTree();
    }

    private void clearRequestErrors() {
        requestOwnerError.setText(" ");
        requestTitleError.setText(" ");
    }

    private void setRequestStatus(String message) {
        requestStatusLabel.setText(message == null ? " " : message);
    }

    private void onSaveRequest() {
        clearRequestErrors();
        boolean valid = true;

        Integer propertyId = null;
        Integer unitId = null;
        if (propertyRadio.isSelected()) {
            PropertyOption selected = (PropertyOption) propertyCombo.getSelectedItem();
            if (selected == null) {
                requestOwnerError.setText("Choose a property.");
                valid = false;
            } else {
                propertyId = selected.property.getId();
            }
        } else {
            UnitOption selected = (UnitOption) unitCombo.getSelectedItem();
            if (selected == null) {
                requestOwnerError.setText("Choose a unit.");
                valid = false;
            } else {
                unitId = selected.unit.getId();
            }
        }

        String title = requestTitleField.getText().trim();
        if (title.isEmpty()) {
            requestTitleError.setText("Title is required.");
            valid = false;
        } else if (title.length() > TITLE_MAX_LENGTH) {
            requestTitleError.setText("Title must be " + TITLE_MAX_LENGTH + " characters or fewer.");
            valid = false;
        }

        String description = requestDescriptionField.getText().trim();
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            requestTitleError.setText("Description must be " + DESCRIPTION_MAX_LENGTH + " characters or fewer.");
            valid = false;
        }

        if (!valid) {
            return;
        }
        String descriptionValue = description.isEmpty() ? null : description;
        String status = (String) requestStatusCombo.getSelectedItem();

        try {
            if (editingRequestId == null) {
                Maintenance_Request newRequest =
                        new Maintenance_Request(propertyId, unitId, title, descriptionValue, status);
                requestDAO.createRequest(newRequest);
            } else {
                Maintenance_Request updated = new Maintenance_Request(
                        editingRequestId, propertyId, unitId, title, descriptionValue, status);
                boolean ok = requestDAO.updateRequest(updated);
                if (!ok) {
                    setRequestStatus("This request no longer exists — it may have been deleted elsewhere.");
                }
            }
            clearRequestForm();
            reloadRequests();
        } catch (IllegalArgumentException e) {
            requestOwnerError.setText(e.getMessage());
        } catch (RuntimeException e) {
            setRequestStatus("Could not save request: " + rootMessage(e));
        }
    }

    private void onDeleteRequest() {
        if (editingRequestId == null) {
            setRequestStatus("Select a request from the list first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this request? This also deletes every task and subtask beneath it.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            requestDAO.deleteRequest(editingRequestId);
            clearRequestForm();
            reloadRequests();
        } catch (RuntimeException e) {
            setRequestStatus("Could not delete request: " + rootMessage(e));
        }
    }

    // Task side

    private JComponent buildTaskSide() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        taskTree.setRootVisible(true);
        taskTree.setShowsRootHandles(true);
        panel.add(new JScrollPane(taskTree), BorderLayout.CENTER);
        panel.add(buildTaskForm(), BorderLayout.SOUTH);

        return panel;
    }

    private JComponent buildTaskForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Add Task / Subtask under the selected tree node");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 13f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        form.add(heading, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        taskTitleField.setColumns(16);
        form.add(taskTitleField, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        form.add(taskTitleError, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        form.add(new JLabel("Cost:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(taskCostField, gbc);
        gbc.gridx = 1;
        gbc.gridy = 4;
        form.add(taskCostError, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(addTaskButton);
        buttons.add(deleteTaskButton);
        buttons.add(computeStatsButton);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        form.add(buttons, gbc);

        gbc.gridy = 6;
        taskStatusLabel.setForeground(new Color(178, 34, 34));
        form.add(taskStatusLabel, gbc);

        gbc.gridy = 7;
        statsLabel.setForeground(new Color(30, 90, 30));
        form.add(statsLabel, gbc);

        return form;
    }

    // Rebuilds the whole tree for the currently selected request from the database.
    private void reloadTaskTree() {
        if (selectedRequest == null) {
            resetTaskTree();
            return;
        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(selectedRequest);
        try {
            List<MaintenanceTask> topLevel = taskDAO.getTopLevelTasksByRequestId(selectedRequest.getId());
            for (MaintenanceTask task : topLevel) {
                root.add(buildTaskNode(task));
            }
        } catch (RuntimeException e) {
            setTaskStatus("Could not load tasks: " + rootMessage(e));
        }
        treeModel.setRoot(root);
        expandAll(taskTree, new TreePath(root));
        updateTaskButtonsEnablement();
        setTaskStatus(null);
        statsLabel.setText(" ");
    }

    // Recursively builds a tree node for this task plus every subtask beneath it -
    // the same base case / recursive case shape as MaintenanceTaskDAO.computeStats,
    // just building a UI tree instead of folding numbers together.
    private DefaultMutableTreeNode buildTaskNode(MaintenanceTask task) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(task);
        List<MaintenanceTask> children = taskDAO.getSubtasks(task.getId());
        for (MaintenanceTask child : children) {
            node.add(buildTaskNode(child));
        }
        return node;
    }

    private void resetTaskTree() {
        treeModel.setRoot(new DefaultMutableTreeNode("Select a request"));
        setTaskStatus(null);
        statsLabel.setText(" ");
        updateTaskButtonsEnablement();
    }

    private static void expandAll(JTree tree, TreePath path) {
        tree.expandPath(path);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++) {
            expandAll(tree, path.pathByAddingChild(node.getChildAt(i)));
        }
    }

    private void updateTaskButtonsEnablement() {
        boolean hasSelectedTask = getSelectedTask() != null;
        boolean hasRequest = selectedRequest != null;
        addTaskButton.setEnabled(hasRequest);
        deleteTaskButton.setEnabled(hasSelectedTask);
        computeStatsButton.setEnabled(hasSelectedTask);
    }

    // Returns the MaintenanceTask behind the selected tree node, or null if the
    // root (the request itself) or nothing is selected.
    private MaintenanceTask getSelectedTask() {
        TreePath path = taskTree.getSelectionPath();
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        return (userObject instanceof MaintenanceTask) ? (MaintenanceTask) userObject : null;
    }

    private void clearTaskErrors() {
        taskTitleError.setText(" ");
        taskCostError.setText(" ");
    }

    private void setTaskStatus(String message) {
        taskStatusLabel.setText(message == null ? " " : message);
    }

    // Creates a new MaintenanceTask. If a task node is selected, the new task
    // becomes its subtask (parentTaskId = selected task's id). If nothing is
    // selected (or the root/request node is), the new task is top-level under
    // the current request (parentTaskId = null).
    private void onAddTask() {
        clearTaskErrors();
        if (selectedRequest == null) {
            setTaskStatus("Select a request first.");
            return;
        }
        boolean valid = true;

        String title = taskTitleField.getText().trim();
        if (title.isEmpty()) {
            taskTitleError.setText("Title is required.");
            valid = false;
        } else if (title.length() > TITLE_MAX_LENGTH) {
            taskTitleError.setText("Title must be " + TITLE_MAX_LENGTH + " characters or fewer.");
            valid = false;
        }

        BigDecimal cost = null;
        try {
            cost = new BigDecimal(taskCostField.getText().trim());
            if (cost.signum() < 0) {
                taskCostError.setText("Cost cannot be negative.");
                valid = false;
            }
        } catch (NumberFormatException e) {
            taskCostError.setText("Enter a valid amount, e.g. 75.00");
            valid = false;
        }

        if (!valid) {
            return;
        }

        MaintenanceTask parent = getSelectedTask();

        try {
            // The factory decides which kind of task this is and builds it correctly -
            // this panel no longer needs to know the construction rules (null parent
            // for top-level, inherited maintenanceRequestId for subtasks) itself.
            MaintenanceTask newTask = (parent == null)
                    ? taskFactory.createTopLevelTask(selectedRequest.getId(), title, cost)
                    : taskFactory.createSubtask(parent.getId(), title, cost);

            taskDAO.createTask(newTask);
            taskTitleField.setText("");
            taskCostField.setText("");
            reloadTaskTree();
        } catch (IllegalArgumentException e) {
            setTaskStatus("Could not create task: " + e.getMessage());
        } catch (RuntimeException e) {
            setTaskStatus("Could not create task: " + rootMessage(e));
        }
    }

    private void onDeleteTask() {
        MaintenanceTask task = getSelectedTask();
        if (task == null) {
            setTaskStatus("Select a task in the tree first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this task? Any subtasks beneath it are deleted too.",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            taskDAO.deleteTask(task.getId());
            reloadTaskTree();
        } catch (RuntimeException e) {
            setTaskStatus("Could not delete task: " + rootMessage(e));
        }
    }

    // Runs the recursive MaintenanceTaskDAO.computeStats() on the selected task
    // and displays the result - total cost, completed/outstanding, % complete
    // across that task and every subtask beneath it.
    private void onComputeStats() {
        MaintenanceTask task = getSelectedTask();
        if (task == null) {
            setTaskStatus("Select a task in the tree first.");
            return;
        }
        try {
            MaintenanceStats stats = taskDAO.computeStats(task.getId());
            statsLabel.setText(String.format(
                    "Total cost: %s | Completed: %d | Outstanding: %d | %.1f%% complete",
                    stats.getTotalCost(), stats.getCompletedCount(), stats.getOutstandingCount(),
                    stats.getCompletionPercentage()));
        } catch (RuntimeException e) {
            setTaskStatus("Could not compute stats: " + rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : t.getMessage();
    }

    private static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(new Color(178, 34, 34));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        return label;
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

    private static class RequestTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Title", "Status"};
        private List<Maintenance_Request> rows = List.of();

        void setRows(List<Maintenance_Request> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        Maintenance_Request getRowAt(int row) {
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
            Maintenance_Request r = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return r.getId();
                case 1: return r.getTitle();
                case 2: return r.getStatus();
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
