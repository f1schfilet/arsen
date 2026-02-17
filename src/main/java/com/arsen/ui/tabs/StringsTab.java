package com.arsen.ui.tabs;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StringsTab extends JPanel {
    private final JTable table;
    private final DefaultTableModel tableModel;

    public StringsTab() {
        setLayout(new BorderLayout());

        String[] columns = {"Index", "String"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setStrings(List<String> strings) {
        tableModel.setRowCount(0);

        for (int i = 0; i < strings.size(); i++) {
            Object[] row = {i, strings.get(i)};
            tableModel.addRow(row);
        }
    }
}
