package com.arsen.ui.panels;

import com.arsen.core.analysis.AnalysisResult;
import com.arsen.model.disassembly.CrossReference;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class RightBottomPanel extends JPanel {
    private final JTabbedPane tabbedPane;
    private final JTextArea outputArea;
    private final JTable xrefTable;
    private final DefaultTableModel xrefTableModel;
    private AnalysisResult analysisResult;

    public RightBottomPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 250));

        tabbedPane = new JTabbedPane();

        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        outputArea.setEditable(false);
        outputArea.setText("Output/Log panel ready\n");
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        tabbedPane.addTab("Output", outputScrollPane);

        String[] xrefColumns = {"From", "To", "Type"};
        xrefTableModel = new DefaultTableModel(xrefColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        xrefTable = new JTable(xrefTableModel);
        xrefTable.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane xrefScrollPane = new JScrollPane(xrefTable);
        tabbedPane.addTab("Cross-references", xrefScrollPane);

        JTextArea registersArea = new JTextArea();
        registersArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        registersArea.setEditable(false);
        registersArea.setText("Registers panel placeholder\n");
        JScrollPane registersScrollPane = new JScrollPane(registersArea);
        tabbedPane.addTab("Registers", registersScrollPane);

        JTextArea memoryArea = new JTextArea();
        memoryArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        memoryArea.setEditable(false);
        memoryArea.setText("Memory view placeholder\n");
        JScrollPane memoryScrollPane = new JScrollPane(memoryArea);
        tabbedPane.addTab("Memory", memoryScrollPane);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void setAnalysisResult(AnalysisResult result) {
        this.analysisResult = result;
        updateCrossReferences();
        appendOutput("Analysis completed: " + result.getFunctions().size() + " functions found\n");
    }

    private void updateCrossReferences() {
        xrefTableModel.setRowCount(0);

        if (analysisResult != null && analysisResult.getCrossReferences() != null) {
            int count = Math.min(analysisResult.getCrossReferences().size(), 1000);
            for (int i = 0; i < count; i++) {
                CrossReference xref = analysisResult.getCrossReferences().get(i);
                Object[] row = {xref.getFrom().toString(), xref.getTo().toString(), xref.getType().toString()};
                xrefTableModel.addRow(row);
            }
            if (analysisResult.getCrossReferences().size() > 1000) {
                Object[] row = {"...", "... " + (analysisResult.getCrossReferences().size() - 1000) + " more", ""};
                xrefTableModel.addRow(row);
            }
        }
    }

    public void appendOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text);
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }
}
