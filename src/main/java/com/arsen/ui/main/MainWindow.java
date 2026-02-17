package com.arsen.ui.main;

import com.arsen.core.analysis.AnalysisResult;
import com.arsen.core.event.Event;
import com.arsen.core.event.EventBus;
import com.arsen.core.event.EventListener;
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

        JMenuItem graphViewItem = new JMenuItem("Graph View");
        graphViewItem.addActionListener(e -> workspace.showGraphViewTab());
        graphViewItem.setAccelerator(KeyStroke.getKeyStroke("control G"));
        viewMenu.add(graphViewItem);

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
                        fileActions.openFile(files.getFirst().toPath());
                    }
                    dtde.dropComplete(true);
                } catch (Exception ex) {
                    log.error("Drag and drop failed", ex);
                    dtde.dropComplete(false);
                }
            }
        });
    }

    private void analyzeBinary() {
        if (binaryService.getCurrentBinary() == null) {
            JOptionPane.showMessageDialog(this, "No binary loaded", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        statusBar.setStatus("Analyzing binary...");
        binaryService.analyze().thenAccept(result -> {
            SwingUtilities.invokeLater(() -> {
                statusBar.setStatus("Analysis complete");
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                statusBar.setStatus("Analysis failed");
                log.error("Analysis failed", ex);
            });
            return null;
        });
    }

    @Override
    public void onEvent(Event event) {
        switch (event.type()) {
            case BINARY_LOADED:
                SwingUtilities.invokeLater(() -> {
                    workspace.setBinary(binaryService.getCurrentBinary());
                    leftSidebar.setBinary(binaryService.getCurrentBinary());
                    statusBar.setStatus("Binary loaded: " + binaryService.getCurrentBinary().getFilePath().getFileName());
                });
                break;

            case ANALYSIS_STARTED:
                SwingUtilities.invokeLater(() -> statusBar.setStatus("Analysis started..."));
                break;

            case ANALYSIS_PROGRESS:
                Integer progress = (Integer) event.payload();
                SwingUtilities.invokeLater(() -> statusBar.setStatus("Analysis progress: " + progress + "%"));
                break;

            case ANALYSIS_COMPLETED:
                AnalysisResult result = (AnalysisResult) event.payload();
                SwingUtilities.invokeLater(() -> {
                    workspace.setAnalysisResult(result);
                    leftSidebar.setAnalysisResult(result);
                    rightBottomPanel.setAnalysisResult(result);
                    statusBar.setStatus("Analysis completed: " + result.getFunctions().size() + " functions found");
                });
                break;

            case ERROR_OCCURRED:
                String error = event.payload() != null ? event.payload().toString() : "Unknown error";
                SwingUtilities.invokeLater(() -> {
                    statusBar.setStatus("Error: " + error);
                    JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
                });
                break;

            default:
                break;
        }
    }
}
