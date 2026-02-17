package com.arsen.disassembler.arch.x86;

import com.arsen.disassembler.IDisassembler;
import com.arsen.model.Address;
import com.arsen.model.Architecture;
import com.arsen.model.disassembly.Instruction;
import com.arsen.model.disassembly.InstructionType;
import com.arsen.model.disassembly.Operand;
import com.arsen.model.disassembly.OperandType;

public record X86Disassembler(Architecture architecture) implements IDisassembler {

    @Override
    public Instruction disassemble(Address address, byte[] data, int offset) {
        if (offset >= data.length) {
            return createInvalidInstruction(address, data, offset);
        }

        int opcode = data[offset] & 0xFF;

        return switch (opcode) {
            case 0x90 -> createNop(address, data, offset);
            case 0xC3 -> createRet(address, data, offset);
            case 0xE8 -> createCall(address, data, offset);
            case 0xE9 -> createJump(address, data, offset);
            case 0x55 -> createPush(address, data, offset, "rbp");
            case 0x5D -> createPop(address, data, offset, "rbp");
            case 0x50, 0x51, 0x52, 0x53, 0x54, 0x56, 0x57 ->
                    createPush(address, data, offset, getRegisterName(opcode - 0x50));
            case 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5E, 0x5F ->
                    createPop(address, data, offset, getRegisterName(opcode - 0x58));
            default -> createGenericInstruction(address, data, offset, opcode);
        };
    }

    private Instruction createNop(Address address, byte[] data, int offset) {
        return Instruction.builder().address(address).bytes(new byte[]{data[offset]}).mnemonic("nop").size(1).type(InstructionType.NOP).build();
    }

    private Instruction createRet(Address address, byte[] data, int offset) {
        return Instruction.builder().address(address).bytes(new byte[]{data[offset]}).mnemonic("ret").size(1).type(InstructionType.RETURN).build();
    }

    private Instruction createCall(Address address, byte[] data, int offset) {
        if (offset + 5 > data.length) {
            return createInvalidInstruction(address, data, offset);
        }

        byte[] instrBytes = new byte[5];
        System.arraycopy(data, offset, instrBytes, 0, 5);

        int displacement = ((data[offset + 4] & 0xFF) << 24) | ((data[offset + 3] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 1] & 0xFF);

        Address targetAddr = address.add(5 + displacement);

        return Instruction.builder().address(address).bytes(instrBytes).mnemonic("call").operand(Operand.builder().type(OperandType.IMMEDIATE).text(targetAddr.toString()).value(targetAddr.value()).build()).size(5).type(InstructionType.CALL).targetAddress(targetAddr).build();
    }

    private Instruction createJump(Address address, byte[] data, int offset) {
        if (offset + 5 > data.length) {
            return createInvalidInstruction(address, data, offset);
        }

        byte[] instrBytes = new byte[5];
        System.arraycopy(data, offset, instrBytes, 0, 5);

        int displacement = ((data[offset + 4] & 0xFF) << 24) | ((data[offset + 3] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 1] & 0xFF);

        Address targetAddr = address.add(5 + displacement);

        return Instruction.builder().address(address).bytes(instrBytes).mnemonic("jmp").operand(Operand.builder().type(OperandType.IMMEDIATE).text(targetAddr.toString()).value(targetAddr.value()).build()).size(5).type(InstructionType.JUMP).targetAddress(targetAddr).build();
    }

    private Instruction createPush(Address address, byte[] data, int offset, String register) {
        return Instruction.builder().address(address).bytes(new byte[]{data[offset]}).mnemonic("push").operand(Operand.builder().type(OperandType.REGISTER).text(register).build()).size(1).type(InstructionType.NORMAL).build();
    }

    private Instruction createPop(Address address, byte[] data, int offset, String register) {
        return Instruction.builder().address(address).bytes(new byte[]{data[offset]}).mnemonic("pop").operand(Operand.builder().type(OperandType.REGISTER).text(register).build()).size(1).type(InstructionType.NORMAL).build();
    }

    private Instruction createGenericInstruction(Address address, byte[] data, int offset, int opcode) {
        return Instruction.builder().address(address).bytes(new byte[]{data[offset]}).mnemonic(String.format("db 0x%02X", opcode)).size(1).type(InstructionType.NORMAL).build();
    }

    private Instruction createInvalidInstruction(Address address, byte[] data, int offset) {
        return Instruction.builder().address(address).bytes(new byte[]{0}).mnemonic("invalid").size(1).type(InstructionType.NORMAL).build();
    }

    private String getRegisterName(int index) {
        String[] registers = architecture == Architecture.X86_64 ? new String[]{"rax", "rcx", "rdx", "rbx", "rsp", "rbp", "rsi", "rdi"} : new String[]{"eax", "ecx", "edx", "ebx", "esp", "ebp", "esi", "edi"};
        return index < registers.length ? registers[index] : "r" + index;
    }

    @Override
    public int getMaxInstructionSize() {
        return 15;
    }
}
