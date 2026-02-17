package com.arsen.ui.tabs;

import com.arsen.core.analysis.AnalysisResult;
import com.arsen.model.Address;
import com.arsen.model.binary.BinaryFile;
import com.arsen.model.disassembly.Instruction;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DisassemblyTab extends JPanel {
    private final JTable table;
    private final DefaultTableModel tableModel;
    private BinaryFile binary;

    public DisassemblyTab() {
        setLayout(new BorderLayout());

        String[] columns = {"Address", "Bytes", "Instruction"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setBinary(BinaryFile binary) {
        this.binary = binary;
        tableModel.setRowCount(0);
    }

    public void setAnalysisResult(AnalysisResult result) {
        tableModel.setRowCount(0);

        List<Instruction> instructions = new ArrayList<>(result.getInstructions().values());
        instructions.sort((a, b) -> a.getAddress().compareTo(b.getAddress()));

        for (Instruction instr : instructions) {
            Object[] row = {instr.getAddress().toString(), instr.getBytesAsHex(), instr.getFullText()};
            tableModel.addRow(row);
        }
    }

    public void navigateToAddress(Address address) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String addrStr = (String) tableModel.getValueAt(i, 0);
            if (addrStr.equals(address.toString())) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                break;
            }
        }
    }
}
