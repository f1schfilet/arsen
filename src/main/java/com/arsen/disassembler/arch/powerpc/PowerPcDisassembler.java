package com.arsen.disassembler.arch.powerpc;

import com.arsen.disassembler.IDisassembler;
import com.arsen.model.Address;
import com.arsen.model.Architecture;
import com.arsen.model.disassembly.Instruction;
import com.arsen.model.disassembly.InstructionType;

public record PowerPcDisassembler(Architecture architecture) implements IDisassembler {

    @Override
    public Instruction disassemble(Address address, byte[] data, int offset) {
        if (offset + 4 > data.length) {
            return createInvalidInstruction(address);
        }

        int instrWord = ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);

        byte[] instrBytes = new byte[4];
        System.arraycopy(data, offset, instrBytes, 0, 4);

        if (instrWord == 0x60000000) {
            return Instruction.builder().address(address).bytes(instrBytes).mnemonic("nop").size(4).type(InstructionType.NOP).build();
        }

        return Instruction.builder().address(address).bytes(instrBytes).mnemonic(String.format(".long 0x%08X", instrWord)).size(4).type(InstructionType.NORMAL).build();
    }

    private Instruction createInvalidInstruction(Address address) {
        return Instruction.builder().address(address).bytes(new byte[]{0, 0, 0, 0}).mnemonic("invalid").size(4).type(InstructionType.NORMAL).build();
    }

    @Override
    public int getMaxInstructionSize() {
        return 4;
    }
}
