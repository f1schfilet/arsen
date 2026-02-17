package com.arsen.core.analysis;

import com.arsen.disassembler.DisassemblerFactory;
import com.arsen.disassembler.IDisassembler;
import com.arsen.model.Address;
import com.arsen.model.Section;
import com.arsen.model.disassembly.Function;
import com.arsen.model.disassembly.Instruction;
import com.arsen.model.disassembly.InstructionType;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

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
        return Function.builder().address(address).name(String.format("sub_%X", address.value())).size(0).build();
    }
}
