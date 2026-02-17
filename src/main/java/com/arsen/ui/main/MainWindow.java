package com.arsen.ui.main;

import com.arsen.core.event.Event;
import com.arsen.core.event.EventBus;
import com.arsen.core.event.EventListener;
import com.arsen.core.event.EventType;
import com.arsen.service.BinaryService;
import com.arsen.ui.actions.FileActions;
import com.arsen.ui.components.StatusBar;
import com.arsen.ui.panels.LeftSidebarPanel;
import com.arsen.ui.panels.RightBottomPanel;
import com.arsen.ui.tabs.TabbedWorkspace;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class MainWindow extends JFrame implements EventListener {
    private final BinaryService binaryService;
    private final EventBus eventBus;
    private final TabbedWorkspace workspace;
    private final StatusBar statusBar;
    private final FileActions fileActions;
    private final LeftSidebarPanel leftSidebar;
    private final RightBottomPanel rightBottomPanel;

    public MainWindow(BinaryService binaryService) {
        this.binaryService = binaryService;
        this.eventBus = EventBus.getInstance();
        this.workspace = new TabbedWorkspace();
        this.statusBar = new StatusBar();
        this.fileActions = new FileActions(this, binaryService);
        this.leftSidebar = new LeftSidebarPanel();
        this.rightBottomPanel = new RightBottomPanel();
        initializeUI();
        setupDragAndDrop();
        eventBus.subscribe(this);
    }

    private void initializeUI() {
        setTitle("Arsen");
        setSize(1600, 1000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setJMenuBar(createMenuBar());
        setLayout(new BorderLayout());

        JSplitPane mainSplitPane = createMainLayout();
        add(mainSplitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        setupLookAndFeel();
    }

    private JSplitPane createMainLayout() {
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftSplit.setLeftComponent(leftSidebar);
        leftSplit.setDividerLocation(250);
        leftSplit.setResizeWeight(0.0);

        JSplitPane centerRightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerRightSplit.setTopComponent(workspace);
        centerRightSplit.setBottomComponent(rightBottomPanel);
        centerRightSplit.setDividerLocation(700);
        centerRightSplit.setResizeWeight(0.8);

        leftSplit.setRightComponent(centerRightSplit);

        return leftSplit;
    }

    private void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            log.warn("Failed to set system look and feel", e);
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(fileActions.getOpenAction());
        fileMenu.addSeparator();
        fileMenu.add(fileActions.getSaveProjectAction());
        fileMenu.add(fileActions.getLoadProjectAction());
        fileMenu.addSeparator();
        fileMenu.add(fileActions.getExitAction());
        menuBar.add(fileMenu);

        JMenu analysisMenu = new JMenu("Analysis");
        JMenuItem analyzeItem = new JMenuItem("Analyze Binary");
        analyzeItem.addActionListener(e -> analyzeBinary());
        analysisMenu.add(analyzeItem);
        menuBar.add(analysisMenu);

        JMenu viewMenu = new JMenu("View");
        JMenuItem disassemblyItem = new JMenuItem("Disassembly");
        disassemblyItem.addActionListener(e -> workspace.showDisassemblyTab());
        viewMenu.add(disassemblyItem);

        JMenuItem pseudocodeItem = new JMenuItem("Pseudocode");
        pseudocodeItem.addActionListener(e -> workspace.showPseudocodeTab());
        pseudocodeItem.setAccelerator(KeyStroke.getKeyStroke("control P"));
        viewMenu.add(pseudocodeItem);

        JMenuItem hexItem = new JMenuItem("Hex View");
        hexItem.addActionListener(e -> workspace.showHexTab());
        viewMenu.add(hexItem);

        JMenuItem stringsItem = new JMenuItem("Strings");
        stringsItem.addActionListener(e -> workspace.showStringsTab());
        viewMenu.add(stringsItem);

        menuBar.add(viewMenu);

        return menuBar;
    }

    private void setupDragAndDrop() {
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    if (!files.isEmpty()) {
                        loadBinary(files.getFirst().toPath());
                    }
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    log.error("Error handling dropped file", e);
                    dtde.dropComplete(false);
                }
            }
        });
    }

    private void loadBinary(Path path) {
        statusBar.setStatus("Loading binary: " + path.getFileName());

        binaryService.loadBinary(path).thenAccept(binary -> SwingUtilities.invokeLater(() -> {
            statusBar.setStatus("Binary loaded successfully");
            workspace.setBinary(binary);
            leftSidebar.setBinary(binary);
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                statusBar.setStatus("Failed to load binary");
                JOptionPane.showMessageDialog(this, "Failed to load binary: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            });
            return null;
        });
    }

    private void analyzeBinary() {
        if (binaryService.getCurrentBinary() == null) {
            JOptionPane.showMessageDialog(this, "Please load a binary first", "No Binary Loaded", JOptionPane.WARNING_MESSAGE);
            return;
        }

        statusBar.setStatus("Analyzing binary...");

        binaryService.analyze().thenAccept(result -> SwingUtilities.invokeLater(() -> {
            statusBar.setStatus("Analysis completed");
            workspace.setAnalysisResult(result);
            leftSidebar.setAnalysisResult(result);
            rightBottomPanel.setAnalysisResult(result);
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                statusBar.setStatus("Analysis failed");
                JOptionPane.showMessageDialog(this, "Analysis failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            });
            return null;
        });
    }

    @Override
    public void onEvent(Event event) {
        SwingUtilities.invokeLater(() -> {
            if (event.type() == EventType.ANALYSIS_PROGRESS) {
                int progress = (Integer) event.payload();
                statusBar.setStatus("Analysis progress: " + progress + "%");
            } else if (event.type() == EventType.ERROR_OCCURRED) {
                statusBar.setStatus("Error: " + event.payload());
            } else if (event.type() == EventType.PSEUDOCODE_GENERATED) {
                statusBar.setStatus("Pseudocode generated");
            }
        });
    }
}
