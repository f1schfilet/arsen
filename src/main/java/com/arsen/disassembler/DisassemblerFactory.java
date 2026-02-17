package com.arsen.disassembler;

import com.arsen.disassembler.arch.arm.ArmDisassembler;
import com.arsen.disassembler.arch.mips.MipsDisassembler;
import com.arsen.disassembler.arch.powerpc.PowerPcDisassembler;
import com.arsen.disassembler.arch.x86.X86Disassembler;
import com.arsen.model.Architecture;

public class DisassemblerFactory {

    public static IDisassembler create(Architecture architecture) {
        return switch (architecture) {
            case X86, X86_64 -> new X86Disassembler(architecture);
            case ARM, ARM64 -> new ArmDisassembler(architecture);
            case MIPS -> new MipsDisassembler(architecture);
            case POWERPC -> new PowerPcDisassembler(architecture);
            default -> throw new UnsupportedOperationException("No disassembler for architecture: " + architecture);
        };
    }
}
