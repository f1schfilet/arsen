package com.arsen.core.pseudocode;

import com.arsen.model.Address;
import com.arsen.model.disassembly.*;

import java.util.*;

public class PseudocodeGenerator {

    public String generatePseudocode(Function function) {
        try {
            return generatePseudocodeInternal(function);
        } catch (Throwable t) {
            return generateFallback(function);
        }
    }

    private String generatePseudocodeInternal(Function function) {
        StringBuilder sb = new StringBuilder();
        Indenter indenter = new Indenter(4);

        String functionName = function.getName() != null ? function.getName() : String.format("func_%016X", function.getAddress().value());
        List<BasicBlock> basicBlocks = new ArrayList<>(function.getBasicBlocks());
        basicBlocks.sort(Comparator.comparing(BasicBlock::getStartAddress));

        Map<Address, BasicBlock> blockByAddress = new HashMap<>();
        for (BasicBlock block : basicBlocks) {
            blockByAddress.put(block.getStartAddress(), block);
        }

        ControlFlowGraph cfg = ControlFlowGraph.build(basicBlocks);

        sb.append("int ").append(functionName).append("()").append("\n");
        sb.append("{").append("\n");
        indenter.indent();

        VariableContext varCtx = new VariableContext();

        List<String> locals = varCtx.getAllVariables();
        if (!locals.isEmpty()) {
            for (String local : locals) {
                appendIndent(sb, indenter);
                sb.append("int ").append(local).append(";").append("\n");
            }
            sb.append("\n");
        }

        StructuredRegion region = StructureBuilder.buildStructuredRegion(cfg, function.getAddress());
        emitRegion(region, sb, indenter, varCtx);

        indenter.unindent();
        sb.append("}").append("\n");
        return sb.toString();
    }

    private void emitRegion(StructuredRegion region, StringBuilder sb, Indenter indenter, VariableContext varCtx) {
        if (region == null) {
            return;
        }
        switch (region.type()) {
            case SEQUENCE:
                for (StructuredRegion child : region.children()) {
                    emitRegion(child, sb, indenter, varCtx);
                }
                break;
            case BASIC_BLOCK:
                emitBasicBlock(region.block(), sb, indenter, varCtx);
                break;
            case IF_THEN:
                emitIfThen(region, sb, indenter, varCtx);
                break;
            case IF_THEN_ELSE:
                emitIfThenElse(region, sb, indenter, varCtx);
                break;
            case WHILE_LOOP:
                emitWhileLoop(region, sb, indenter, varCtx);
                break;
            case DO_WHILE_LOOP:
                emitDoWhileLoop(region, sb, indenter, varCtx);
                break;
            case INFINITE_LOOP:
                emitInfiniteLoop(region, sb, indenter, varCtx);
                break;
            default:
                emitLinearFallback(region, sb, indenter, varCtx);
                break;
        }
    }

    private void emitBasicBlock(BasicBlock block, StringBuilder sb, Indenter indenter, VariableContext varCtx) {
        if (block == null) {
            return;
        }
        for (Instruction instr : block.getInstructions()) {
            emitInstruction(instr, sb, indenter, varCtx);
        }
    }

    private void emitInstruction(Instruction instr, StringBuilder sb, Indenter indenter, VariableContext varCtx) {
        if (instr.getType() == InstructionType.RETURN) {
            appendIndent(sb, indenter);
            if (!instr.getOperands().isEmpty()) {
                Operand op = instr.getOperands().get(0);
                String expr = buildExpressionFromOperand(op, varCtx);
                sb.append("return ").append(expr).append(";").append("\n");
            } else {
                sb.append("return;").append("\n");
            }
            return;
        }
        if (instr.getType() == InstructionType.NOP) {
            return;
        }
        if (instr.getType() == InstructionType.CALL) {
            appendIndent(sb, indenter);
            String callExpr = buildCallExpression(instr, varCtx);
            sb.append(callExpr).append(";").append("\n");
            return;
        }
        if (instr.getType() == InstructionType.JUMP || instr.getType() == InstructionType.CONDITIONAL_JUMP) {
            return;
        }
        appendIndent(sb, indenter);
        String expr = buildHighLevelExpression(instr, varCtx);
        if (expr == null || expr.isEmpty()) {
            sb.append(";").append("\n");
        } else {
            sb.append(expr).append(";").append("\n");
        }
    }

    private String buildHighLevelExpression(Instruction instr, VariableContext varCtx) {
        String mnemonic = instr.getMnemonic().toLowerCase();
        List<Operand> ops = instr.getOperands();

        if (mnemonic.startsWith("mov") && ops.size() == 2) {
            String dst = buildLValue(ops.get(0), varCtx);
            String src = buildExpressionFromOperand(ops.get(1), varCtx);
            return dst + " = " + src;
        }

        if ((mnemonic.startsWith("add") || mnemonic.startsWith("sub") || mnemonic.startsWith("mul") || mnemonic.startsWith("imul") || mnemonic.startsWith("and") || mnemonic.startsWith("or") || mnemonic.startsWith("xor")) && ops.size() == 2) {
            String dst = buildLValue(ops.get(0), varCtx);
            String src = buildExpressionFromOperand(ops.get(1), varCtx);
            String op = operatorForMnemonic(mnemonic);
            return dst + " = " + dst + " " + op + " " + src;
        }

        if ((mnemonic.startsWith("cmp") || mnemonic.startsWith("test")) && ops.size() == 2) {
            String left = buildExpressionFromOperand(ops.get(0), varCtx);
            String right = buildExpressionFromOperand(ops.get(1), varCtx);
            return "/* compare " + left + " and " + right + " */";
        }

        if (mnemonic.startsWith("inc") && ops.size() == 1) {
            String dst = buildLValue(ops.get(0), varCtx);
            return dst + " = " + dst + " + 1";
        }

        if (mnemonic.startsWith("dec") && ops.size() == 1) {
            String dst = buildLValue(ops.get(0), varCtx);
            return dst + " = " + dst + " - 1";
        }

        return "";
    }

    private String operatorForMnemonic(String mnemonic) {
        if (mnemonic.startsWith("add")) {
            return "+";
        }
        if (mnemonic.startsWith("sub")) {
            return "-";
        }
        if (mnemonic.startsWith("mul") || mnemonic.startsWith("imul")) {
            return "*";
        }
        if (mnemonic.startsWith("and")) {
            return "&";
        }
        if (mnemonic.startsWith("or")) {
            return "|";
        }
        if (mnemonic.startsWith("xor")) {
            return "^";
        }
        return "+";
    }

    private String buildCallExpression(Instruction instr, VariableContext varCtx) {
        List<Operand> ops = instr.getOperands();
        String functionName;
        if (instr.getTargetAddress() != null) {
            functionName = String.format("sub_%016X", instr.getTargetAddress().value());
        } else if (!ops.isEmpty()) {
            functionName = buildExpressionFromOperand(ops.get(0), varCtx);
        } else {
            functionName = "unknown_call";
        }
        List<String> args = new ArrayList<>();
        for (int i = 1; i < ops.size(); i++) {
            args.add(buildExpressionFromOperand(ops.get(i), varCtx));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(functionName).append("(");
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(args.get(i));
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildLValue(Operand op, VariableContext varCtx) {
        if (op.getType() == OperandType.REGISTER) {
            return varCtx.variableForRegister(op.getText());
        }
        if (op.getType() == OperandType.MEMORY) {
            return varCtx.variableForMemory(op.getText(), op.getValue());
        }
        if (op.getType() == OperandType.DISPLACEMENT) {
            return varCtx.variableForStackOffset(op.getValue());
        }
        return buildExpressionFromOperand(op, varCtx);
    }

    private String buildExpressionFromOperand(Operand op, VariableContext varCtx) {
        if (op.getType() == OperandType.REGISTER) {
            return varCtx.variableForRegister(op.getText());
        }
        if (op.getType() == OperandType.IMMEDIATE) {
            return String.valueOf(op.getValue());
        }
        if (op.getType() == OperandType.MEMORY) {
            String base = op.getText();
            String var = varCtx.variableForMemory(base, op.getValue());
            return var;
        }
        if (op.getType() == OperandType.DISPLACEMENT) {
            return varCtx.variableForStackOffset(op.getValue());
        }
        return op.getText();
    }

    private void emitIfThen(StructuredRegion region, StringBuilder sb, Indenter indenter, VariableContext varCtx) {
        ConditionInfo cond = region.condition();
        appendIndent(sb, indenter);
        sb.append("if (").append(cond.expression()).append(")").append("\n");
        appendIndent(sb, indenter);
        sb.append("{").append("\n");
        indenter.indent();
        for (StructuredRegion child : region.children()) {
            emitRegion(child, sb, indenter, varCtx);
        }
        indenter.unindent();
        appendIndent(sb, indenter);
        sb.append("}").append("\n");
    }

    private void emitIfThenElse(StructuredRegion region, StringBuilder sb, Indenter indenter, VariableContext varCtx) {
        ConditionInfo cond = region.condition();
        appendIndent(sb, indenter);
        sb.append("if (").append(cond.expression()).append(")").append("\n");
        appendIndent(sb, indenter);
        sb.append("{").append("\n");
        indenter.indent();
        for (StructuredRegion child : region.trueChildren()) {
            emitRegion(child, sb, indenter, varCtx);
        }
        indenter.unindent();
        appendIndent(sb, indenter);
        sb.append("}").append("\n");
        appendIndent(sb, indenter);
        sb.append("else").append("\n");
        appendIndent(sb, indenter);
        sb.append("{").append("\n");
        indenter.indent();
        for (StructuredRegion child : region.falseChildren()) {
            emitRegion(child, sb, indenter, varCtx);
        }
        indenter.unindent();
        appendIndent(sb, indenter);
        sb.append("}").append("\n");
    }

    private void emitWhileLoop(StructuredRegion region, StringBuilder sb, Indenter indenter, VariableContext varCtx) {
        ConditionInfo cond = region.condition();
        appendIndent(sb, indenter);
        sb.append("while (").append(cond.expression()).append(")").append("\n");
        appendIndent(sb, indenter);
        sb.append("{").append("\n");
        indenter.indent();
        for (StructuredRegion child : region.children()) {
            emitRegion(child, sb, indenter, varCtx);
        }
        indenter.unindent();
        appendIndent(sb, indenter);
        sb.append("}").append("\n");
    }

    private void emitDoWhileLoop(StructuredRegion region, StringBuilder sb, Indenter indenter, VariableContext varCtx) {
        appendIndent(sb, indenter);
        sb.append("do").append("\n");
        appendIndent(sb, indenter);
        sb.append("{").append("\n");
        indenter.indent();
        for (StructuredRegion child : region.children()) {
            emitRegion(child, sb, indenter, varCtx);
        }
        indenter.unindent();
        appendIndent(sb, indenter);
        sb.append("} while (").append(region.condition().expression()).append(");").append("\n");
    }

    private void emitInfiniteLoop(StructuredRegion region, StringBuilder sb, Indenter indenter, VariableContext varCtx) {
        appendIndent(sb, indenter);
        sb.append("while (true)").append("\n");
        appendIndent(sb, indenter);
        sb.append("{").append("\n");
        indenter.indent();
        for (StructuredRegion child : region.children()) {
            emitRegion(child, sb, indenter, varCtx);
        }
        indenter.unindent();
        appendIndent(sb, indenter);
        sb.append("}").append("\n");
    }

    private void emitLinearFallback(StructuredRegion region, StringBuilder sb, Indenter indenter, VariableContext varCtx) {
        Deque<StructuredRegion> stack = new ArrayDeque<>();
        stack.push(region);
        while (!stack.isEmpty()) {
            StructuredRegion current = stack.pop();
            if (current.type() == StructuredRegionType.BASIC_BLOCK) {
                emitBasicBlock(current.block(), sb, indenter, varCtx);
            } else {
                List<StructuredRegion> children = new ArrayList<>(current.children());
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(children.get(i));
                }
            }
        }
    }

    private String generateFallback(Function function) {
        StringBuilder sb = new StringBuilder();
        String functionName = function.getName() != null ? function.getName() : String.format("func_%016X", function.getAddress().value());
        sb.append("int ").append(functionName).append("()").append("\n");
        sb.append("{").append("\n");
        sb.append("    ").append("while (true)").append("\n");
        sb.append("    ").append("{").append("\n");
        sb.append("        ").append("break;").append("\n");
        sb.append("    ").append("}").append("\n");
        sb.append("}").append("\n");
        return sb.toString();
    }

    private void appendIndent(StringBuilder sb, Indenter indenter) {
        sb.append(indenter.currentIndent());
    }
}

class Indenter {

    private final int spacesPerLevel;
    private int level;

    Indenter(int spacesPerLevel) {
        this.spacesPerLevel = spacesPerLevel;
        this.level = 0;
    }

    void indent() {
        level++;
    }

    void unindent() {
        if (level > 0) {
            level--;
        }
    }

    String currentIndent() {
        int count = spacesPerLevel * level;
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}

class VariableContext {

    private final Map<String, String> registerVars;
    private final Map<Long, String> stackVars;
    private final Map<String, String> memoryVars;
    private int nextLocalIndex;

    VariableContext() {
        this.registerVars = new HashMap<>();
        this.stackVars = new HashMap<>();
        this.memoryVars = new HashMap<>();
        this.nextLocalIndex = 0;
    }

    String variableForRegister(String regName) {
        String key = regName.toLowerCase();
        String existing = registerVars.get(key);
        if (existing != null) {
            return existing;
        }
        String name = "r_" + key;
        registerVars.put(key, name);
        return name;
    }

    String variableForStackOffset(long offset) {
        Long key = offset;
        String existing = stackVars.get(key);
        if (existing != null) {
            return existing;
        }
        String name = "local_" + nextLocalIndex;
        nextLocalIndex++;
        stackVars.put(key, name);
        return name;
    }

    String variableForMemory(String desc, long value) {
        String key = desc + ":" + value;
        String existing = memoryVars.get(key);
        if (existing != null) {
            return existing;
        }
        String name = "mem_" + nextLocalIndex;
        nextLocalIndex++;
        memoryVars.put(key, name);
        return name;
    }

    List<String> getAllVariables() {
        HashSet<String> result = new HashSet<>();
        result.addAll(registerVars.values());
        result.addAll(stackVars.values());
        result.addAll(memoryVars.values());
        ArrayList<String> list = new ArrayList<>(result);
        list.sort(String::compareTo);
        return list;
    }
}

class ControlFlowGraph {

    private final Map<Address, BasicBlock> blocks;
    private final Map<Address, List<Address>> successors;
    private final Map<Address, List<Address>> predecessors;

    private ControlFlowGraph(Map<Address, BasicBlock> blocks, Map<Address, List<Address>> successors, Map<Address, List<Address>> predecessors) {
        this.blocks = blocks;
        this.successors = successors;
        this.predecessors = predecessors;
    }

    static ControlFlowGraph build(List<BasicBlock> basicBlocks) {
        Map<Address, BasicBlock> blockMap = new HashMap<>();
        Map<Address, List<Address>> succ = new HashMap<>();
        Map<Address, List<Address>> pred = new HashMap<>();

        for (BasicBlock block : basicBlocks) {
            Address start = block.getStartAddress();
            blockMap.put(start, block);
            ArrayList<Address> succList = new ArrayList<>(block.getSuccessors());
            succList.sort(Address::compareTo);
            succ.put(start, succList);
        }

        for (Map.Entry<Address, List<Address>> e : succ.entrySet()) {
            Address from = e.getKey();
            for (Address to : e.getValue()) {
                List<Address> p = pred.computeIfAbsent(to, k -> new ArrayList<>());
                p.add(from);
            }
        }

        for (List<Address> list : pred.values()) {
            list.sort(Address::compareTo);
        }

        return new ControlFlowGraph(blockMap, succ, pred);
    }

    BasicBlock getBlock(Address addr) {
        return blocks.get(addr);
    }

    List<Address> getSuccessors(Address addr) {
        List<Address> s = successors.get(addr);
        if (s == null) {
            return List.of();
        }
        return s;
    }

    List<Address> getPredecessors(Address addr) {
        List<Address> p = predecessors.get(addr);
        if (p == null) {
            return List.of();
        }
        return p;
    }

    Map<Address, BasicBlock> getBlocks() {
        return blocks;
    }
}

enum StructuredRegionType {
    SEQUENCE, BASIC_BLOCK, IF_THEN, IF_THEN_ELSE, WHILE_LOOP, DO_WHILE_LOOP, INFINITE_LOOP, UNKNOWN
}

record ConditionInfo(String expression) {

}

record StructuredRegion(StructuredRegionType type, BasicBlock block, List<StructuredRegion> children,
                        ConditionInfo condition, List<StructuredRegion> trueChildren,
                        List<StructuredRegion> falseChildren) {

    StructuredRegion(StructuredRegionType type, BasicBlock block, List<StructuredRegion> children, ConditionInfo condition, List<StructuredRegion> trueChildren, List<StructuredRegion> falseChildren) {
        this.type = type;
        this.block = block;
        this.children = children != null ? children : List.of();
        this.condition = condition;
        this.trueChildren = trueChildren != null ? trueChildren : List.of();
        this.falseChildren = falseChildren != null ? falseChildren : List.of();
    }

    static StructuredRegion basicBlock(BasicBlock block) {
        return new StructuredRegion(StructuredRegionType.BASIC_BLOCK, block, List.of(), null, null, null);
    }

    static StructuredRegion sequence(List<StructuredRegion> children) {
        return new StructuredRegion(StructuredRegionType.SEQUENCE, null, children, null, null, null);
    }

    static StructuredRegion ifThen(ConditionInfo cond, List<StructuredRegion> thenChildren) {
        return new StructuredRegion(StructuredRegionType.IF_THEN, null, thenChildren, cond, null, null);
    }

    static StructuredRegion ifThenElse(ConditionInfo cond, List<StructuredRegion> thenChildren, List<StructuredRegion> elseChildren) {
        return new StructuredRegion(StructuredRegionType.IF_THEN_ELSE, null, List.of(), cond, thenChildren, elseChildren);
    }

    static StructuredRegion whileLoop(ConditionInfo cond, List<StructuredRegion> body) {
        return new StructuredRegion(StructuredRegionType.WHILE_LOOP, null, body, cond, null, null);
    }

    static StructuredRegion doWhileLoop(ConditionInfo cond, List<StructuredRegion> body) {
        return new StructuredRegion(StructuredRegionType.DO_WHILE_LOOP, null, body, cond, null, null);
    }

    static StructuredRegion infiniteLoop(List<StructuredRegion> body) {
        return new StructuredRegion(StructuredRegionType.INFINITE_LOOP, null, body, null, null, null);
    }

    static StructuredRegion unknown(List<StructuredRegion> children) {
        return new StructuredRegion(StructuredRegionType.UNKNOWN, null, children, null, null, null);
    }
}

class StructureBuilder {

    static StructuredRegion buildStructuredRegion(ControlFlowGraph cfg, Address entry) {
        List<Address> order = new ArrayList<>(cfg.getBlocks().keySet());
        order.sort(Address::compareTo);
        List<StructuredRegion> regions = new ArrayList<>();
        for (Address addr : order) {
            BasicBlock block = cfg.getBlock(addr);
            regions.add(StructuredRegion.basicBlock(block));
        }
        return StructuredRegion.sequence(regions);
    }
}
