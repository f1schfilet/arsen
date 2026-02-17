package com.arsen.ui.tabs;

import com.arsen.core.analysis.AnalysisResult;
import com.arsen.model.binary.BinaryFile;
import lombok.Getter;

import javax.swing.*;

@Getter
public class TabbedWorkspace extends JTabbedPane {
    private final DisassemblyTab disassemblyTab;
    private final GraphViewTab graphViewTab;
    private final PseudocodeTab pseudocodeTab;
    private final HexViewTab hexViewTab;
    private final StringsTab stringsTab;
    private final SectionsTab sectionsTab;

    public TabbedWorkspace() {
        this.disassemblyTab = new DisassemblyTab();
        this.graphViewTab = new GraphViewTab();
        this.pseudocodeTab = new PseudocodeTab();
        this.hexViewTab = new HexViewTab();
        this.stringsTab = new StringsTab();
        this.sectionsTab = new SectionsTab();

        addTab("Disassembly", disassemblyTab);
        addTab("Graph View", graphViewTab);
        addTab("Pseudocode", pseudocodeTab);
        addTab("Hex View", hexViewTab);
        addTab("Strings", stringsTab);
        addTab("Sections", sectionsTab);
    }

    public void setBinary(BinaryFile binary) {
        disassemblyTab.setBinary(binary);
        graphViewTab.setBinary(binary);
        pseudocodeTab.setBinary(binary);
        hexViewTab.setBinary(binary);
        sectionsTab.setBinary(binary);
    }

    public void setAnalysisResult(AnalysisResult result) {
        disassemblyTab.setAnalysisResult(result);
        graphViewTab.setAnalysisResult(result);
        pseudocodeTab.setAnalysisResult(result);
        stringsTab.setStrings(result.getStrings());
    }

    public void showDisassemblyTab() {
        setSelectedComponent(disassemblyTab);
    }

    public void showGraphViewTab() {
        setSelectedComponent(graphViewTab);
    }

    public void showPseudocodeTab() {
        setSelectedComponent(pseudocodeTab);
    }

    public void showHexTab() {
        setSelectedComponent(hexViewTab);
    }

    public void showStringsTab() {
        setSelectedComponent(stringsTab);
    }

    public void showSectionsTab() {
        setSelectedComponent(sectionsTab);
    }
}
