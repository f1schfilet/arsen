package com.arsen.ui.components;

import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel {
    private final JLabel statusLabel;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        setPreferredSize(new Dimension(getWidth(), 25));

        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        add(statusLabel, BorderLayout.WEST);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }
}
