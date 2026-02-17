package com.arsen.core.analysis;

import com.arsen.disassembler.DisassemblerFactory;
import com.arsen.disassembler.IDisassembler;
import com.arsen.model.Address;
import com.arsen.model.Section;
import com.arsen.model.disassembly.BasicBlock;
import com.arsen.model.disassembly.Function;
import com.arsen.model.disassembly.Instruction;
import com.arsen.model.disassembly.InstructionType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class FunctionDetectionPass implements AnalysisPass {

    @Override
    public String getName() {
        return "Function Detection";
    }

    @Override
    public void execute(AnalysisContext context) {
        IDisassembler disassembler = DisassemblerFactory.create(context.getBinaryFile().getArchitecture());

        Set<Address> functionStarts = new HashSet<>();
        functionStarts.add(context.getBinaryFile().getEntryPoint());

        for (Section section : context.getBinaryFile().getSections()) {
            if (section.isExecutable()) {
                analyzeSection(section, disassembler, context, functionStarts);
            }
        }

        for (Address funcAddr : functionStarts) {
            Function function = analyzeFunction(funcAddr, disassembler, context);
            if (function != null) {
                context.addFunction(function);
            }
        }
    }

    private void analyzeSection(Section section, IDisassembler disassembler, AnalysisContext context, Set<Address> functionStarts) {
        byte[] data = section.getData();
        if (data == null) return;

        Address currentAddr = section.getVirtualAddress();
        int offset = 0;

        while (offset < data.length) {
            try {
                Instruction instruction = disassembler.disassemble(currentAddr, data, offset);
                context.addInstruction(instruction);

                if (instruction.getType() == InstructionType.CALL && instruction.getTargetAddress() != null) {
                    functionStarts.add(instruction.getTargetAddress());
                }

                offset += instruction.getSize();
                currentAddr = currentAddr.add(instruction.getSize());
            } catch (Exception e) {
                offset++;
                currentAddr = currentAddr.add(1);
            }
        }
    }

    private Function analyzeFunction(Address address, IDisassembler disassembler, AnalysisContext context) {
        Map<Address, Instruction> allInstructions = context.getInstructions();

        Instruction startInstr = allInstructions.get(address);
        if (startInstr == null) {
            return null;
        }

        Set<Address> visited = new HashSet<>();
        Set<Address> toVisit = new LinkedHashSet<>();
        toVisit.add(address);

        List<Instruction> functionInstructions = new ArrayList<>();

        while (!toVisit.isEmpty()) {
            Iterator<Address> iterator = toVisit.iterator();
            Address current = iterator.next();
            iterator.remove();

            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            Instruction instr = allInstructions.get(current);
            if (instr == null) {
                continue;
            }

            functionInstructions.add(instr);

            if (instr.getType() == InstructionType.RETURN) {
                continue;
            }

            if (instr.getType() == InstructionType.JUMP || instr.getType() == InstructionType.CONDITIONAL_JUMP) {
                if (instr.getTargetAddress() != null) {
                    toVisit.add(instr.getTargetAddress());
                }
            }

            if (instr.getType() != InstructionType.JUMP) {
                Address nextAddr = current.add(instr.getSize());
                if (allInstructions.containsKey(nextAddr)) {
                    toVisit.add(nextAddr);
                }
            }

            if (functionInstructions.size() > 10000) {
                break;
            }
        }

        if (functionInstructions.isEmpty()) {
            return Function.builder().address(address).name(formatFunctionName(address)).size(0).build();
        }

        functionInstructions.sort(Comparator.comparing(Instruction::getAddress));

        List<BasicBlock> basicBlocks = buildBasicBlocks(functionInstructions);

        long functionSize = 0;
        if (!functionInstructions.isEmpty()) {
            Instruction first = functionInstructions.getFirst();
            Instruction last = functionInstructions.getLast();
            functionSize = last.getAddress().value() - first.getAddress().value() + last.getSize();
        }

        return Function.builder().address(address).name(formatFunctionName(address)).size(functionSize).basicBlocks(basicBlocks).build();
    }

    private List<BasicBlock> buildBasicBlocks(List<Instruction> instructions) {
        if (instructions.isEmpty()) {
            return List.of();
        }

        Set<Address> blockStarts = new HashSet<>();
        blockStarts.add(instructions.getFirst().getAddress());

        for (Instruction instr : instructions) {
            if (instr.getType() == InstructionType.JUMP || instr.getType() == InstructionType.CONDITIONAL_JUMP || instr.getType() == InstructionType.RETURN) {

                Address nextAddr = instr.getAddress().add(instr.getSize());
                blockStarts.add(nextAddr);

                if (instr.getTargetAddress() != null) {
                    blockStarts.add(instr.getTargetAddress());
                }
            }
        }

        Map<Address, BasicBlock.BasicBlockBuilder> blockBuilders = new HashMap<>();
        BasicBlock.BasicBlockBuilder currentBuilder = null;
        Address currentBlockStart = null;

        for (Instruction instr : instructions) {
            Address addr = instr.getAddress();

            if (blockStarts.contains(addr)) {
                if (currentBuilder != null) {
                    blockBuilders.put(currentBlockStart, currentBuilder);
                }

                currentBlockStart = addr;
                currentBuilder = BasicBlock.builder().startAddress(addr);
            }

            if (currentBuilder != null) {
                currentBuilder.instruction(instr);
                currentBuilder.endAddress(addr.add(instr.getSize()));
            }

            if (instr.getType() == InstructionType.JUMP || instr.getType() == InstructionType.CONDITIONAL_JUMP || instr.getType() == InstructionType.RETURN) {

                if (currentBuilder != null) {
                    blockBuilders.put(currentBlockStart, currentBuilder);
                    currentBuilder = null;
                    currentBlockStart = null;
                }
            }
        }

        if (currentBuilder != null) {
            blockBuilders.put(currentBlockStart, currentBuilder);
        }

        for (Map.Entry<Address, BasicBlock.BasicBlockBuilder> entry : blockBuilders.entrySet()) {
            BasicBlock.BasicBlockBuilder builder = entry.getValue();
            BasicBlock tempBlock = builder.build();

            if (tempBlock.getInstructions().isEmpty()) {
                continue;
            }

            Instruction lastInstr = tempBlock.getInstructions().getLast();

            if (lastInstr.getType() == InstructionType.JUMP || lastInstr.getType() == InstructionType.CONDITIONAL_JUMP) {
                if (lastInstr.getTargetAddress() != null) {
                    builder.successor(lastInstr.getTargetAddress());
                }
            }

            if (lastInstr.getType() == InstructionType.CONDITIONAL_JUMP || lastInstr.getType() == InstructionType.NORMAL || lastInstr.getType() == InstructionType.CALL) {
                Address fallthrough = lastInstr.getAddress().add(lastInstr.getSize());
                if (blockStarts.contains(fallthrough)) {
                    builder.successor(fallthrough);
                }
            }
        }

        for (Map.Entry<Address, BasicBlock.BasicBlockBuilder> entry : blockBuilders.entrySet()) {
            Address blockAddr = entry.getKey();
            BasicBlock.BasicBlockBuilder builder = entry.getValue();
            BasicBlock tempBlock = builder.build();

            for (Address succ : tempBlock.getSuccessors()) {
                BasicBlock.BasicBlockBuilder succBuilder = blockBuilders.get(succ);
                if (succBuilder != null) {
                    succBuilder.predecessor(blockAddr);
                }
            }
        }

        List<BasicBlock> result = new ArrayList<>();
        for (BasicBlock.BasicBlockBuilder builder : blockBuilders.values()) {
            result.add(builder.build());
        }

        result.sort(Comparator.comparing(BasicBlock::getStartAddress));

        return result;
    }

    private String formatFunctionName(Address address) {
        return String.format("SUB_%016X", address.value()).toUpperCase();
    }
}
