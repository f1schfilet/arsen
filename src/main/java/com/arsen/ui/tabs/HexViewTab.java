package com.arsen.ui.tabs;

import com.arsen.model.binary.BinaryFile;

import javax.swing.*;
import java.awt.*;

public class HexViewTab extends JPanel {
    private final JTextArea textArea;
    private BinaryFile binary;

    public HexViewTab() {
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setBinary(BinaryFile binary) {
        this.binary = binary;
        displayHexDump(binary.getRawData());
    }

    private void displayHexDump(byte[] data) {
        if (data == null) return;

        StringBuilder sb = new StringBuilder();
        int displayLimit = Math.min(data.length, 10000);

        for (int i = 0; i < displayLimit; i += 16) {
            sb.append(String.format("%08X: ", i));

            for (int j = 0; j < 16; j++) {
                if (i + j < displayLimit) {
                    sb.append(String.format("%02X ", data[i + j] & 0xFF));
                } else {
                    sb.append("   ");
                }
            }

            sb.append(" ");

            for (int j = 0; j < 16 && i + j < displayLimit; j++) {
                byte b = data[i + j];
                char c = (b >= 32 && b <= 126) ? (char) b : '.';
                sb.append(c);
            }

            sb.append("\n");
        }

        if (data.length > displayLimit) {
            sb.append("\n... (showing first ").append(displayLimit).append(" bytes of ").append(data.length).append(")");
        }

        textArea.setText(sb.toString());
        textArea.setCaretPosition(0);
    }
}
