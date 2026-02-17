package com.arsen.ui.tabs;

import com.arsen.core.analysis.AnalysisResult;
import com.arsen.core.pseudocode.PseudocodeService;
import com.arsen.model.Address;
import com.arsen.model.binary.BinaryFile;
import com.arsen.model.disassembly.Function;
import com.arsen.ui.components.PseudocodeHighlighter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class PseudocodeTab extends JPanel {
    private final JTextPane textPane;
    private final JComboBox<FunctionItem> functionSelector;
    private final StyledDocument document;
    private final PseudocodeHighlighter highlighter;
    private final PseudocodeService pseudocodeService;
    private BinaryFile binary;
    private AnalysisResult analysisResult;

    public PseudocodeTab() {
        this.pseudocodeService = PseudocodeService.getInstance();
        this.document = new DefaultStyledDocument();
        this.textPane = new JTextPane(document);
        this.highlighter = new PseudocodeHighlighter(document);
        this.functionSelector = new JComboBox<>();

        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        textPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
        textPane.setEditable(false);
        textPane.setBackground(new Color(250, 250, 250));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel label = new JLabel("Function: ");
        topPanel.add(label, BorderLayout.WEST);

        functionSelector.addActionListener(e -> onFunctionSelected());
        topPanel.add(functionSelector, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
    }

    public void setBinary(BinaryFile binary) {
        this.binary = binary;
        functionSelector.removeAllItems();
        clearDisplay();
    }

    public void setAnalysisResult(AnalysisResult result) {
        this.analysisResult = result;
        populateFunctionSelector(result);
    }

    private void populateFunctionSelector(AnalysisResult result) {
        functionSelector.removeAllItems();

        if (result == null || result.getFunctions().isEmpty()) {
            clearDisplay();
            return;
        }

        List<Function> functions = new ArrayList<>(result.getFunctions().values());
        functions.sort(Comparator.comparing(Function::getAddress));

        for (Function function : functions) {
            functionSelector.addItem(new FunctionItem(function));
        }

        if (functionSelector.getItemCount() > 0) {
            functionSelector.setSelectedIndex(0);
        }
    }

    private void onFunctionSelected() {
        FunctionItem selectedItem = (FunctionItem) functionSelector.getSelectedItem();
        if (selectedItem == null) {
            clearDisplay();
            return;
        }

        Function function = selectedItem.function;
        displayPseudocode(function);
    }

    private void displayPseudocode(Function function) {
        try {
            String pseudocode = pseudocodeService.generatePseudocode(function);

            if (pseudocode == null || pseudocode.trim().isEmpty()) {
                displayError("No pseudocode available for function: " + function.getName());
                return;
            }

            highlighter.highlight(pseudocode);
            textPane.setCaretPosition(0);
        } catch (Exception e) {
            log.error("Failed to generate pseudocode for function {}", function.getName(), e);
            displayError("Failed to generate pseudocode: " + e.getMessage());
        }
    }

    private void clearDisplay() {
        try {
            document.remove(0, document.getLength());
        } catch (Exception e) {
            log.error("Failed to clear display", e);
        }
    }

    private void displayError(String message) {
        try {
            document.remove(0, document.getLength());
            document.insertString(0, message, null);
        } catch (Exception e) {
            log.error("Failed to display error", e);
        }
    }

    public void navigateToFunction(Address address) {
        if (analysisResult == null) return;

        Function function = analysisResult.getFunctions().get(address);
        if (function == null) return;

        for (int i = 0; i < functionSelector.getItemCount(); i++) {
            FunctionItem item = functionSelector.getItemAt(i);
            if (item.function.getAddress().equals(address)) {
                functionSelector.setSelectedIndex(i);
                break;
            }
        }
    }

    private record FunctionItem(Function function) {
        @Override
        public String toString() {
            return String.format("%s (%s)", function.getName() != null ? function.getName() : "unnamed", function.getAddress().toString());
        }
    }
}
