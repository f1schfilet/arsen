package com.arsen.ui.panels;

import com.arsen.core.analysis.AnalysisResult;
import com.arsen.model.Export;
import com.arsen.model.Import;
import com.arsen.model.binary.BinaryFile;
import com.arsen.model.disassembly.Function;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeftSidebarPanel extends JPanel {
    private final JTree tree;
    private final DefaultMutableTreeNode root;
    private final DefaultTreeModel treeModel;
    private BinaryFile binary;
    private AnalysisResult analysisResult;

    public LeftSidebarPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(250, 0));

        root = new DefaultMutableTreeNode("Project");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);

        JScrollPane scrollPane = new JScrollPane(tree);
        add(scrollPane, BorderLayout.CENTER);

        initializeEmptyTree();
    }

    private void initializeEmptyTree() {
        root.removeAllChildren();
        root.add(new DefaultMutableTreeNode("Functions"));
        root.add(new DefaultMutableTreeNode("Strings"));
        root.add(new DefaultMutableTreeNode("Imports"));
        root.add(new DefaultMutableTreeNode("Exports"));
        treeModel.reload();
    }

    public void setBinary(BinaryFile binary) {
        this.binary = binary;
        updateTree();
    }

    public void setAnalysisResult(AnalysisResult result) {
        this.analysisResult = result;
        updateTree();
    }

    private void updateTree() {
        root.removeAllChildren();

        DefaultMutableTreeNode functionsNode = new DefaultMutableTreeNode("Functions");
        if (analysisResult != null && analysisResult.getFunctions() != null) {
            List<Function> functions = new ArrayList<>(analysisResult.getFunctions().values());
            functions.sort(Comparator.comparing(Function::getAddress));
            for (Function function : functions) {
                String displayName = function.getName() != null ? function.getName() : "unnamed";
                functionsNode.add(new DefaultMutableTreeNode(displayName + " - " + function.getAddress().toString()));
            }
        }
        root.add(functionsNode);

        DefaultMutableTreeNode stringsNode = new DefaultMutableTreeNode("Strings");
        if (analysisResult != null && analysisResult.getStrings() != null) {
            int count = Math.min(analysisResult.getStrings().size(), 100);
            for (int i = 0; i < count; i++) {
                String str = analysisResult.getStrings().get(i);
                String displayStr = str.length() > 50 ? str.substring(0, 50) + "..." : str;
                stringsNode.add(new DefaultMutableTreeNode(displayStr));
            }
            if (analysisResult.getStrings().size() > 100) {
                stringsNode.add(new DefaultMutableTreeNode("... " + (analysisResult.getStrings().size() - 100) + " more"));
            }
        }
        root.add(stringsNode);

        DefaultMutableTreeNode importsNode = new DefaultMutableTreeNode("Imports");
        if (binary != null && binary.getImports() != null) {
            for (Import imp : binary.getImports()) {
                String displayName = imp.getLibrary() + "." + imp.getName();
                importsNode.add(new DefaultMutableTreeNode(displayName));
            }
        }
        root.add(importsNode);

        DefaultMutableTreeNode exportsNode = new DefaultMutableTreeNode("Exports");
        if (binary != null && binary.getExports() != null) {
            for (Export exp : binary.getExports()) {
                exportsNode.add(new DefaultMutableTreeNode(exp.getName() + " - " + exp.getAddress().toString()));
            }
        }
        root.add(exportsNode);

        treeModel.reload();

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }
}
