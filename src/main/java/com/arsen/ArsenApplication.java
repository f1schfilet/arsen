package com.arsen;

import com.arsen.plugin.PluginContext;
import com.arsen.plugin.PluginManager;
import com.arsen.service.BinaryService;
import com.arsen.ui.main.MainWindow;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

@Slf4j
public class ArsenApplication {

    static void main(String[] args) {
        log.info("Starting Arsen application");

        SwingUtilities.invokeLater(() -> {
            try {
                BinaryService binaryService = new BinaryService();

                PluginContext pluginContext = new PluginContext(binaryService, com.arsen.core.event.EventBus.getInstance());
                PluginManager pluginManager = new PluginManager(pluginContext);

                MainWindow mainWindow = new MainWindow(binaryService);
                mainWindow.setVisible(true);

                log.info("Arsen application started successfully");
            } catch (Exception e) {
                log.error("Failed to start application", e);
                JOptionPane.showMessageDialog(null, "Failed to start application: " + e.getMessage(), "Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
