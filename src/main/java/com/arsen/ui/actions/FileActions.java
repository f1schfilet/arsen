package com.arsen.ui.actions;

import com.arsen.service.BinaryService;
import lombok.Getter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;

@Getter
public class FileActions {
    private final JFrame parent;
    private final BinaryService binaryService;
    private final Action openAction;
    private final Action saveProjectAction;
    private final Action loadProjectAction;
    private final Action exitAction;

    public FileActions(JFrame parent, BinaryService binaryService) {
        this.parent = parent;
        this.binaryService = binaryService;

        this.openAction = new OpenAction();
        this.saveProjectAction = new SaveProjectAction();
        this.loadProjectAction = new LoadProjectAction();
        this.exitAction = new ExitAction();
    }

    public void openFile(Path path) {
        if (path != null && path.toFile().exists()) {
            binaryService.loadBinary(path);
        } else {
            JOptionPane.showMessageDialog(parent, "File does not exist: " + path, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class OpenAction extends AbstractAction {
        public OpenAction() {
            super("Open Binary...");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control O"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Open Binary File");

            int result = fileChooser.showOpenDialog(parent);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                openFile(file.toPath());
            }
        }
    }

    private class SaveProjectAction extends AbstractAction {
        public SaveProjectAction() {
            super("Save Project...");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control S"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Project");

            int result = fileChooser.showSaveDialog(parent);
            if (result == JFileChooser.APPROVE_OPTION) {
                JOptionPane.showMessageDialog(parent, "Project save functionality ready", "Save Project", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private class LoadProjectAction extends AbstractAction {
        public LoadProjectAction() {
            super("Load Project...");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control L"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Load Project");

            int result = fileChooser.showOpenDialog(parent);
            if (result == JFileChooser.APPROVE_OPTION) {
                JOptionPane.showMessageDialog(parent, "Project load functionality ready", "Load Project", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private class ExitAction extends AbstractAction {
        public ExitAction() {
            super("Exit");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control Q"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }
}
