package com.arsen.core.analysis;

import com.arsen.model.disassembly.CrossReference;
import com.arsen.model.disassembly.Instruction;
import com.arsen.model.disassembly.InstructionType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CrossReferencePass implements AnalysisPass {

    @Override
    public String getName() {
        return "Cross Reference Analysis";
    }

    @Override
    public void execute(AnalysisContext context) {
        for (Instruction instruction : context.getInstructions().values()) {
            if (instruction.getTargetAddress() != null) {
                CrossReference.XRefType type = determineXRefType(instruction.getType());
                if (type != null) {
                    CrossReference xref = CrossReference.builder().from(instruction.getAddress()).to(instruction.getTargetAddress()).type(type).build();
                    context.addCrossReference(xref);
                }
            }
        }
    }

    private CrossReference.XRefType determineXRefType(InstructionType instructionType) {
        return switch (instructionType) {
            case CALL -> CrossReference.XRefType.CALL;
            case JUMP, CONDITIONAL_JUMP -> CrossReference.XRefType.JUMP;
            default -> null;
        };
    }
}
