package com.arsen.ui.tabs;

import com.arsen.core.analysis.AnalysisResult;
import com.arsen.core.analysis.graph.ControlFlowGraph;
import com.arsen.core.analysis.graph.ControlFlowGraphBuilder;
import com.arsen.model.Address;
import com.arsen.model.binary.BinaryFile;
import com.arsen.model.disassembly.BasicBlock;
import com.arsen.model.disassembly.Function;
import com.arsen.model.disassembly.Instruction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;

public class GraphViewTab extends JPanel {
    private final JComboBox<FunctionItem> functionSelector;
    private final GraphPanel graphPanel;
    private final JScrollPane scrollPane;
    private BinaryFile binary;
    private AnalysisResult analysisResult;
    private final ControlFlowGraphBuilder cfgBuilder;

    public GraphViewTab() {
        setLayout(new BorderLayout());

        this.cfgBuilder = new ControlFlowGraphBuilder();

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel functionLabel = new JLabel("Function: ");
        functionSelector = new JComboBox<>();
        functionSelector.addActionListener(e -> onFunctionSelected());

        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorPanel.add(functionLabel);
        selectorPanel.add(functionSelector);

        topPanel.add(selectorPanel, BorderLayout.WEST);

        add(topPanel, BorderLayout.NORTH);

        graphPanel = new GraphPanel();
        scrollPane = new JScrollPane(graphPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setBinary(BinaryFile binary) {
        this.binary = binary;
        functionSelector.removeAllItems();
        graphPanel.clear();
    }

    public void setAnalysisResult(AnalysisResult result) {
        this.analysisResult = result;
        functionSelector.removeAllItems();

        if (result != null && result.getFunctions() != null) {
            List<Function> functions = new ArrayList<>(result.getFunctions().values());
            functions.sort(Comparator.comparing(Function::getAddress));

            for (Function function : functions) {
                functionSelector.addItem(new FunctionItem(function));
            }

            if (!functions.isEmpty()) {
                functionSelector.setSelectedIndex(0);
            }
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

    private void onFunctionSelected() {
        FunctionItem selected = (FunctionItem) functionSelector.getSelectedItem();
        if (selected != null) {
            Function function = selected.function;
            ControlFlowGraph cfg = cfgBuilder.build(function);
            graphPanel.setCFG(cfg, function);
        }
    }

    private record FunctionItem(Function function) {
        @Override
        public String toString() {
            return String.format("%s (%s)", function.getName() != null ? function.getName() : "unnamed", function.getAddress().toString());
        }
    }

    private static class GraphPanel extends JPanel {
        private ControlFlowGraph cfg;
        private Function function;
        private final Map<Address, BlockNode> nodePositions;
        private BlockNode selectedNode;
        private double zoomFactor = 1.0;
        private Point dragStart;
        private Point viewOffset = new Point(0, 0);

        private static final int NODE_WIDTH = 250;
        private static final int NODE_MIN_HEIGHT = 60;
        private static final int NODE_PADDING = 10;
        private static final int VERTICAL_SPACING = 80;
        private static final int HORIZONTAL_SPACING = 50;
        private static final int LINE_HEIGHT = 16;

        public GraphPanel() {
            setBackground(Color.WHITE);
            nodePositions = new HashMap<>();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isMiddleMouseButton(e) || (SwingUtilities.isLeftMouseButton(e) && e.isControlDown())) {
                        dragStart = e.getPoint();
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        handleNodeSelection(e.getPoint());
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    dragStart = null;
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        handleNodeDoubleClick(e.getPoint());
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        int dx = e.getX() - dragStart.x;
                        int dy = e.getY() - dragStart.y;
                        viewOffset.translate(dx, dy);
                        dragStart = e.getPoint();
                        repaint();
                    }
                }
            });

            addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    double delta = -e.getPreciseWheelRotation() * 0.1;
                    double newZoom = Math.max(0.3, Math.min(3.0, zoomFactor + delta));
                    if (newZoom != zoomFactor) {
                        zoomFactor = newZoom;
                        updatePreferredSize();
                        repaint();
                    }
                }
            });
        }

        public void clear() {
            cfg = null;
            function = null;
            nodePositions.clear();
            selectedNode = null;
            zoomFactor = 1.0;
            viewOffset = new Point(0, 0);
            repaint();
        }

        public void setCFG(ControlFlowGraph cfg, Function function) {
            this.cfg = cfg;
            this.function = function;
            this.selectedNode = null;
            this.nodePositions.clear();
            this.zoomFactor = 1.0;
            this.viewOffset = new Point(30, 30);

            if (cfg != null && !cfg.isEmpty()) {
                computeLayout();
                updatePreferredSize();
            }

            repaint();
        }

        private void computeLayout() {
            if (cfg == null || cfg.isEmpty()) {
                return;
            }

            Map<Address, Integer> levels = new HashMap<>();
            Map<Integer, List<Address>> levelNodes = new HashMap<>();

            Address entry = cfg.entryBlock();
            if (entry == null) {
                List<Address> allAddresses = new ArrayList<>(cfg.getAllBlockAddresses());
                allAddresses.sort(Address::compareTo);
                entry = allAddresses.isEmpty() ? null : allAddresses.get(0);
            }

            if (entry != null) {
                assignLevels(entry, 0, levels, new HashSet<>());
            }

            for (Address addr : cfg.getAllBlockAddresses()) {
                if (!levels.containsKey(addr)) {
                    assignLevels(addr, 0, levels, new HashSet<>());
                }
            }

            for (Map.Entry<Address, Integer> e : levels.entrySet()) {
                int level = e.getValue();
                levelNodes.computeIfAbsent(level, k -> new ArrayList<>()).add(e.getKey());
            }

            for (List<Address> nodes : levelNodes.values()) {
                nodes.sort(Address::compareTo);
            }

            int maxNodesInLevel = levelNodes.values().stream().mapToInt(List::size).max().orElse(1);

            for (Map.Entry<Integer, List<Address>> e : levelNodes.entrySet()) {
                int level = e.getKey();
                List<Address> nodes = e.getValue();

                int y = level * (NODE_MIN_HEIGHT + VERTICAL_SPACING);

                int totalWidth = nodes.size() * NODE_WIDTH + (nodes.size() - 1) * HORIZONTAL_SPACING;
                int startX = (maxNodesInLevel * (NODE_WIDTH + HORIZONTAL_SPACING) - totalWidth) / 2;

                for (int i = 0; i < nodes.size(); i++) {
                    Address addr = nodes.get(i);
                    BasicBlock block = cfg.getBlock(addr);

                    int lines = calculateBlockLines(block);
                    int height = Math.max(NODE_MIN_HEIGHT, lines * LINE_HEIGHT + 2 * NODE_PADDING);

                    int x = startX + i * (NODE_WIDTH + HORIZONTAL_SPACING);

                    BlockNode node = new BlockNode(addr, block, x, y, NODE_WIDTH, height);
                    nodePositions.put(addr, node);
                }
            }
        }

        private void assignLevels(Address addr, int level, Map<Address, Integer> levels, Set<Address> visited) {
            if (visited.contains(addr)) {
                return;
            }

            visited.add(addr);

            Integer existingLevel = levels.get(addr);
            if (existingLevel == null || existingLevel < level) {
                levels.put(addr, level);

                List<Address> successors = cfg.getSuccessors(addr);
                for (Address succ : successors) {
                    assignLevels(succ, level + 1, levels, visited);
                }
            }
        }

        private int calculateBlockLines(BasicBlock block) {
            if (block == null || block.getInstructions() == null) {
                return 2;
            }
            return block.getInstructions().size() + 1;
        }

        private void updatePreferredSize() {
            if (nodePositions.isEmpty()) {
                setPreferredSize(new Dimension(800, 600));
                return;
            }

            int maxX = 0;
            int maxY = 0;

            for (BlockNode node : nodePositions.values()) {
                int nodeMaxX = (int) ((node.x + node.width) * zoomFactor);
                int nodeMaxY = (int) ((node.y + node.height) * zoomFactor);
                maxX = Math.max(maxX, nodeMaxX);
                maxY = Math.max(maxY, nodeMaxY);
            }

            setPreferredSize(new Dimension(maxX + 100, maxY + 100));
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (cfg == null || cfg.isEmpty()) {
                g.setColor(Color.GRAY);
                String msg = "No control flow graph available";
                FontMetrics fm = g.getFontMetrics();
                int msgWidth = fm.stringWidth(msg);
                g.drawString(msg, (getWidth() - msgWidth) / 2, getHeight() / 2);
                return;
            }

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            AffineTransform originalTransform = g2d.getTransform();

            g2d.translate(viewOffset.x, viewOffset.y);
            g2d.scale(zoomFactor, zoomFactor);

            drawEdges(g2d);
            drawNodes(g2d);

            g2d.setTransform(originalTransform);
            g2d.dispose();
        }

        private void drawEdges(Graphics2D g2d) {
            g2d.setStroke(new BasicStroke(2.0f));

            for (Map.Entry<Address, BlockNode> e : nodePositions.entrySet()) {
                Address fromAddr = e.getKey();
                BlockNode fromNode = e.getValue();

                List<Address> successors = cfg.getSuccessors(fromAddr);

                if (successors.size() == 1) {
                    Address toAddr = successors.get(0);
                    BlockNode toNode = nodePositions.get(toAddr);
                    if (toNode != null) {
                        g2d.setColor(new Color(60, 120, 216));
                        drawEdge(g2d, fromNode, toNode, false);
                    }
                } else if (successors.size() == 2) {
                    Address leftAddr = successors.get(0);
                    Address rightAddr = successors.get(1);

                    BlockNode leftNode = nodePositions.get(leftAddr);
                    BlockNode rightNode = nodePositions.get(rightAddr);

                    if (leftNode != null) {
                        g2d.setColor(new Color(34, 139, 34));
                        drawEdge(g2d, fromNode, leftNode, true);
                    }

                    if (rightNode != null) {
                        g2d.setColor(new Color(220, 20, 60));
                        drawEdge(g2d, fromNode, rightNode, true);
                    }
                } else {
                    for (Address toAddr : successors) {
                        BlockNode toNode = nodePositions.get(toAddr);
                        if (toNode != null) {
                            g2d.setColor(new Color(100, 100, 100));
                            drawEdge(g2d, fromNode, toNode, false);
                        }
                    }
                }
            }
        }

        private void drawEdge(Graphics2D g2d, BlockNode from, BlockNode to, boolean drawArrow) {
            int fromX = from.x + from.width / 2;
            int fromY = from.y + from.height;
            int toX = to.x + to.width / 2;
            int toY = to.y;

            if (to.y <= from.y) {
                fromX = from.x + from.width;
                fromY = from.y + from.height / 2;
                toX = to.x;
                toY = to.y + to.height / 2;
            }

            g2d.draw(new Line2D.Double(fromX, fromY, toX, toY));

            if (drawArrow) {
                drawArrowHead(g2d, fromX, fromY, toX, toY);
            }
        }

        private void drawArrowHead(Graphics2D g2d, int x1, int y1, int x2, int y2) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowSize = 10;

            Path2D arrow = new Path2D.Double();
            arrow.moveTo(x2, y2);
            arrow.lineTo(x2 - arrowSize * Math.cos(angle - Math.PI / 6), y2 - arrowSize * Math.sin(angle - Math.PI / 6));
            arrow.lineTo(x2 - arrowSize * Math.cos(angle + Math.PI / 6), y2 - arrowSize * Math.sin(angle + Math.PI / 6));
            arrow.closePath();

            g2d.fill(arrow);
        }

        private void drawNodes(Graphics2D g2d) {
            for (BlockNode node : nodePositions.values()) {
                boolean isSelected = node == selectedNode;
                boolean isEntry = cfg.entryBlock() != null && cfg.entryBlock().equals(node.address);

                Color bgColor = isSelected ? new Color(255, 250, 205) : (isEntry ? new Color(230, 240, 255) : Color.WHITE);
                Color borderColor = isSelected ? new Color(255, 140, 0) : (isEntry ? new Color(70, 130, 180) : new Color(100, 100, 100));

                g2d.setColor(bgColor);
                g2d.fillRect(node.x, node.y, node.width, node.height);

                g2d.setColor(borderColor);
                g2d.setStroke(new BasicStroke(isSelected ? 3.0f : 2.0f));
                g2d.drawRect(node.x, node.y, node.width, node.height);

                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Monospaced", Font.BOLD, 11));

                String header = node.address.toString();
                g2d.drawString(header, node.x + NODE_PADDING, node.y + NODE_PADDING + 12);

                g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));

                int yOffset = node.y + NODE_PADDING + 12 + LINE_HEIGHT;

                if (node.block != null && node.block.getInstructions() != null) {
                    for (Instruction instr : node.block.getInstructions()) {
                        String instrText = instr.getFullText();
                        if (instrText.length() > 35) {
                            instrText = instrText.substring(0, 32) + "...";
                        }
                        g2d.drawString(instrText, node.x + NODE_PADDING, yOffset);
                        yOffset += LINE_HEIGHT;

                        if (yOffset > node.y + node.height - NODE_PADDING) {
                            break;
                        }
                    }
                }
            }
        }

        private void handleNodeSelection(Point point) {
            Point transformed = transformPoint(point);

            for (BlockNode node : nodePositions.values()) {
                if (node.contains(transformed)) {
                    selectedNode = node;
                    repaint();
                    return;
                }
            }

            selectedNode = null;
            repaint();
        }

        private void handleNodeDoubleClick(Point point) {
            Point transformed = transformPoint(point);

            for (BlockNode node : nodePositions.values()) {
                if (node.contains(transformed)) {
                    return;
                }
            }
        }

        private Point transformPoint(Point point) {
            int x = (int) ((point.x - viewOffset.x) / zoomFactor);
            int y = (int) ((point.y - viewOffset.y) / zoomFactor);
            return new Point(x, y);
        }

        private static class BlockNode {
            Address address;
            BasicBlock block;
            int x, y, width, height;

            BlockNode(Address address, BasicBlock block, int x, int y, int width, int height) {
                this.address = address;
                this.block = block;
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }

            boolean contains(Point p) {
                return p.x >= x && p.x <= x + width && p.y >= y && p.y <= y + height;
            }
        }
    }
}
