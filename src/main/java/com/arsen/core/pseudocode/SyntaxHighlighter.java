package com.arsen.core.pseudocode;

import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

@Slf4j
public class SyntaxHighlighter {

    public String highlight(String rawPseudocode) {
        if (rawPseudocode == null || rawPseudocode.isEmpty()) {
            return rawPseudocode;
        }

        try {
            return highlightWithRSyntaxTextArea(rawPseudocode);
        } catch (Throwable t) {
            log.error("Syntax highlighting failed, returning raw pseudocode", t);
            return rawPseudocode;
        }
    }

    private String highlightWithRSyntaxTextArea(String rawPseudocode) {
        RSyntaxTextArea textArea = new RSyntaxTextArea();

        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
        textArea.setCodeFoldingEnabled(false);
        textArea.setAutoIndentEnabled(false);
        textArea.setEnabled(false);
        textArea.setVisible(false);

        textArea.setText(rawPseudocode);

        String highlightedText = textArea.getText();

        textArea.setText(null);

        if (highlightedText == null || highlightedText.isEmpty()) {
            return rawPseudocode;
        }

        return highlightedText;
    }
}
