package com.Propertmanagement.gui;

import com.Propertmanagement.dao.leasedao;
import com.Propertmanagement.dao.PaymentDAO;
import com.Propertmanagement.model.Lease;
import com.Propertmanagement.model.Payment;

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

// CRUD screen for Payment.
public class PaymentPanel extends JPanel {

    private static final int METHOD_MAX_LENGTH = 50;

    private final PaymentDAO paymentDAO;
    private final leasedao LeaseDAO;

    private final JComboBox<LeaseOption> leaseCombo = new JComboBox<>();
    private final JTextField amountField = new JTextField();
    private final JTextField paymentDateField = new JTextField();
    private final JTextField methodField = new JTextField();

    private final JLabel leaseError = errorLabel();
    private final JLabel amountError = errorLabel();
    private final JLabel paymentDateError = errorLabel();
    private final JLabel methodError = errorLabel();
    private final JLabel statusLabel = new JLabel(" ");

    private final PaymentTableModel tableModel;
    private final JTable table;

    private final JButton saveButton = new JButton("Save");
    private final JButton newButton = new JButton("New");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton refreshLeasesButton = new JButton("Refresh leases");

    private Integer editingId = null;

    public PaymentPanel(PaymentDAO paymentDAO, leasedao LeaseDAO) {
        super(new BorderLayout(12, 12));
        this.paymentDAO = paymentDAO;
        this.LeaseDAO = LeaseDAO;
        this.tableModel = new PaymentTableModel();
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
        refreshLeasesButton.addActionListener(e -> reloadLeases());

        reloadLeases();
        reload();
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("Payment");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        form.add(heading, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Lease:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(leaseCombo, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        form.add(refreshLeasesButton, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(leaseError, gbc);
        gbc.gridwidth = 1;

        addFieldRow(form, gbc, 3, "Amount", amountField, amountError);
        addFieldRow(form, gbc, 5, "Payment Date (yyyy-MM-dd)", paymentDateField, paymentDateError);
        addFieldRow(form, gbc, 7, "Method", methodField, methodError);

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

    private void reloadLeases() {
        LeaseOption previouslySelected = (LeaseOption) leaseCombo.getSelectedItem();
        leaseCombo.removeAllItems();
        tableModel.clearLeaseLabelCache();
        try {
            List<Lease> leases = LeaseDAO.getAllLeases();
            for (Lease lease : leases) {
                leaseCombo.addItem(new LeaseOption(lease));
            }
            setStatus(leases.isEmpty() ? "No leases exist yet — add a lease first." : null);

            if (previouslySelected != null) {
                for (int i = 0; i < leaseCombo.getItemCount(); i++) {
                    if (leaseCombo.getItemAt(i).lease.getId() == previouslySelected.lease.getId()) {
                        leaseCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            setStatus("Could not load leases: " + rootMessage(e));
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        Payment selected = tableModel.getRowAt(table.convertRowIndexToModel(row));
        editingId = selected.getId();
        selectLeaseById(selected.getLeaseId());
        amountField.setText(selected.getAmount().toPlainString());
        paymentDateField.setText(selected.getPaymentDate().toString());
        methodField.setText(selected.getMethod() == null ? "" : selected.getMethod());
        clearErrors();
        setStatus(null);
    }

    private void selectLeaseById(int leaseId) {
        for (int i = 0; i < leaseCombo.getItemCount(); i++) {
            if (leaseCombo.getItemAt(i).lease.getId() == leaseId) {
                leaseCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void clearForm() {
        editingId = null;
        if (leaseCombo.getItemCount() > 0) {
            leaseCombo.setSelectedIndex(0);
        }
        amountField.setText("");
        paymentDateField.setText("");
        methodField.setText("");
        table.clearSelection();
        clearErrors();
        setStatus(null);
    }

    private void clearErrors() {
        leaseError.setText(" ");
        amountError.setText(" ");
        paymentDateError.setText(" ");
        methodError.setText(" ");
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null ? " " : message);
    }

    private void onSave() {
        clearErrors();
        boolean valid = true;

        LeaseOption selectedLease = (LeaseOption) leaseCombo.getSelectedItem();
        if (selectedLease == null) {
            leaseError.setText("Choose a lease — add one first if none are listed.");
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
            amountError.setText("Enter a valid amount, e.g. 450.00");
            valid = false;
        }

        LocalDate paymentDate = null;
        try {
            paymentDate = LocalDate.parse(paymentDateField.getText().trim());
        } catch (DateTimeParseException e) {
            paymentDateError.setText("Enter a valid date as yyyy-MM-dd.");
            valid = false;
        }

        String methodText = methodField.getText().trim();
        if (methodText.length() > METHOD_MAX_LENGTH) {
            methodError.setText("Method must be " + METHOD_MAX_LENGTH + " characters or fewer.");
            valid = false;
        }

        if (!valid) {
            return;
        }

        String methodValue = methodText.isEmpty() ? null : methodText;

        try {
            // Guard against the lease having been deleted since the dropdown was populated
            if (LeaseDAO.getLeaseById(selectedLease.lease.getId()) == null) {
                leaseError.setText("This lease no longer exists. Refreshing the lease list.");
                reloadLeases();
                return;
            }

            if (editingId == null) {
                Payment newPayment = new Payment(selectedLease.lease.getId(), amount, paymentDate, methodValue);
                paymentDAO.createPayment(newPayment);
            } else {
                Payment updated = new Payment(editingId, selectedLease.lease.getId(), amount, paymentDate, methodValue);
                boolean ok = paymentDAO.updatePayment(updated);
                if (!ok) {
                    setStatus("This payment no longer exists — it may have been deleted elsewhere.");
                }
            }
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not save payment: " + rootMessage(e));
        }
    }

    private void onDelete() {
        if (editingId == null) {
            setStatus("Select a payment from the list first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this payment?",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            paymentDAO.deletePayment(editingId);
            clearForm();
            reload();
        } catch (RuntimeException e) {
            setStatus("Could not delete payment: " + rootMessage(e));
        }
    }

    private void reload() {
        try {
            List<Payment> all = paymentDAO.getAllPayments();
            tableModel.setRows(all);
        } catch (RuntimeException e) {
            setStatus("Could not load payments: " + rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : t.getMessage();
    }

    private static class LeaseOption {
        final Lease lease;

        LeaseOption(Lease lease) {
            this.lease = lease;
        }

        @Override
        public String toString() {
            return "Lease #" + lease.getId() + " (" + lease.getStartDate() + " to " + lease.getEndDate() + ")";
        }
    }

    private class PaymentTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Lease", "Amount", "Payment Date", "Method"};
        private List<Payment> rows = List.of();
        private final Map<Integer, String> leaseLabelCache = new HashMap<>();

        void setRows(List<Payment> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        void clearLeaseLabelCache() {
            leaseLabelCache.clear();
        }

        Payment getRowAt(int row) {
            return rows.get(row);
        }

        private String leaseLabel(int leaseId) {
            return leaseLabelCache.computeIfAbsent(leaseId, id -> {
                for (int i = 0; i < leaseCombo.getItemCount(); i++) {
                    LeaseOption option = leaseCombo.getItemAt(i);
                    if (option.lease.getId() == id) {
                        return option.toString();
                    }
                }
                return "Lease id " + id;
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
            Payment p = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return p.getId();
                case 1: return leaseLabel(p.getLeaseId());
                case 2: return p.getAmount();
                case 3: return p.getPaymentDate();
                case 4: return p.getMethod();
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}