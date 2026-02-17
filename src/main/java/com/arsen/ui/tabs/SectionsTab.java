package com.arsen.ui.tabs;

import com.arsen.model.Section;
import com.arsen.model.binary.BinaryFile;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SectionsTab extends JPanel {
    private final JTable table;
    private final DefaultTableModel tableModel;

    public SectionsTab() {
        setLayout(new BorderLayout());

        String[] columns = {"Name", "Virtual Address", "Virtual Size", "Raw Size", "Flags"};
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

    public void setBinary(BinaryFile binary) {
        tableModel.setRowCount(0);

        for (Section section : binary.getSections()) {
            Object[] row = {section.getName(), section.getVirtualAddress().toString(), String.format("0x%X", section.getVirtualSize()), String.format("0x%X", section.getRawSize()), String.format("0x%08X", section.getFlags())};
            tableModel.addRow(row);
        }
    }
}
