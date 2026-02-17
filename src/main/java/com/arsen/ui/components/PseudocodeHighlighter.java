package com.arsen.ui.components;

import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PseudocodeHighlighter {
    private final StyledDocument document;
    private final Map<String, Style> styles;
    private static final String[] KEYWORDS = {"if", "else", "while", "do", "for", "return", "break", "continue", "int", "void"};
    private static final String[] OPERATORS = {"=", "+", "-", "*", "/", "%", "&", "|", "^", "!", "~", "<", ">", "==", "!=", "<=", ">="};

    public PseudocodeHighlighter(StyledDocument document) {
        this.document = document;
        this.styles = new HashMap<>();
        initializeStyles();
    }

    private void initializeStyles() {
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style keywordStyle = document.addStyle("keyword", defaultStyle);
        StyleConstants.setForeground(keywordStyle, new Color(0, 0, 255));
        StyleConstants.setBold(keywordStyle, true);
        styles.put("keyword", keywordStyle);

        Style functionStyle = document.addStyle("function", defaultStyle);
        StyleConstants.setForeground(functionStyle, new Color(128, 0, 128));
        StyleConstants.setBold(functionStyle, true);
        styles.put("function", functionStyle);

        Style variableStyle = document.addStyle("variable", defaultStyle);
        StyleConstants.setForeground(variableStyle, new Color(0, 128, 128));
        styles.put("variable", variableStyle);

        Style numberStyle = document.addStyle("number", defaultStyle);
        StyleConstants.setForeground(numberStyle, new Color(255, 0, 0));
        styles.put("number", numberStyle);

        Style commentStyle = document.addStyle("comment", defaultStyle);
        StyleConstants.setForeground(commentStyle, new Color(0, 128, 0));
        StyleConstants.setItalic(commentStyle, true);
        styles.put("comment", commentStyle);

        Style operatorStyle = document.addStyle("operator", defaultStyle);
        StyleConstants.setForeground(operatorStyle, new Color(0, 0, 0));
        StyleConstants.setBold(operatorStyle, true);
        styles.put("operator", operatorStyle);

        Style bracketStyle = document.addStyle("bracket", defaultStyle);
        StyleConstants.setForeground(bracketStyle, new Color(128, 128, 128));
        StyleConstants.setBold(bracketStyle, true);
        styles.put("bracket", bracketStyle);

        Style stringStyle = document.addStyle("string", defaultStyle);
        StyleConstants.setForeground(stringStyle, new Color(163, 21, 21));
        styles.put("string", stringStyle);
    }

    public void highlight(String text) {
        try {
            document.remove(0, document.getLength());
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                highlightLine(line);
                if (i < lines.length - 1) {
                    document.insertString(document.getLength(), "\n", null);
                }
            }
        } catch (BadLocationException e) {
            insertPlainText(text);
        }
    }

    private void highlightLine(String line) throws BadLocationException {
        if (line.trim().startsWith("//") || line.trim().startsWith("/*")) {
            document.insertString(document.getLength(), line, styles.get("comment"));
            return;
        }

        int pos = 0;
        while (pos < line.length()) {
            char c = line.charAt(pos);

            if (Character.isWhitespace(c)) {
                document.insertString(document.getLength(), String.valueOf(c), null);
                pos++;
                continue;
            }

            if (c == '{' || c == '}' || c == '(' || c == ')' || c == '[' || c == ']' || c == ';') {
                document.insertString(document.getLength(), String.valueOf(c), styles.get("bracket"));
                pos++;
                continue;
            }

            int commentStart = line.indexOf("/*", pos);
            if (commentStart == pos) {
                int commentEnd = line.indexOf("*/", pos + 2);
                if (commentEnd != -1) {
                    String comment = line.substring(pos, commentEnd + 2);
                    document.insertString(document.getLength(), comment, styles.get("comment"));
                    pos = commentEnd + 2;
                    continue;
                }
            }

            if (Character.isDigit(c) || (c == '0' && pos + 1 < line.length() && line.charAt(pos + 1) == 'x')) {
                StringBuilder number = new StringBuilder();
                while (pos < line.length() && (Character.isDigit(line.charAt(pos)) || "xXabcdefABCDEF".indexOf(line.charAt(pos)) != -1)) {
                    number.append(line.charAt(pos));
                    pos++;
                }
                document.insertString(document.getLength(), number.toString(), styles.get("number"));
                continue;
            }

            if (Character.isJavaIdentifierStart(c)) {
                StringBuilder word = new StringBuilder();
                while (pos < line.length() && (Character.isJavaIdentifierPart(line.charAt(pos)) || line.charAt(pos) == '_')) {
                    word.append(line.charAt(pos));
                    pos++;
                }

                String token = word.toString();
                Style style = null;

                for (String keyword : KEYWORDS) {
                    if (token.equals(keyword)) {
                        style = styles.get("keyword");
                        break;
                    }
                }

                if (style == null && (token.startsWith("sub_") || token.startsWith("func_"))) {
                    style = styles.get("function");
                }

                if (style == null && (token.startsWith("r_") || token.startsWith("local_") || token.startsWith("mem_") || token.startsWith("var_"))) {
                    style = styles.get("variable");
                }

                document.insertString(document.getLength(), token, style);
                continue;
            }

            boolean operatorMatched = false;
            for (String op : OPERATORS) {
                if (line.startsWith(op, pos)) {
                    document.insertString(document.getLength(), op, styles.get("operator"));
                    pos += op.length();
                    operatorMatched = true;
                    break;
                }
            }

            if (!operatorMatched) {
                document.insertString(document.getLength(), String.valueOf(c), null);
                pos++;
            }
        }
    }

    private void insertPlainText(String text) {
        try {
            document.insertString(document.getLength(), text, null);
        } catch (BadLocationException e) {
        }
    }
}
