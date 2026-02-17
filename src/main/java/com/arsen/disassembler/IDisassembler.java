package com.arsen.disassembler;

import com.arsen.model.Address;
import com.arsen.model.Architecture;
import com.arsen.model.disassembly.Instruction;

public interface IDisassembler {
    Architecture architecture();

    Instruction disassemble(Address address, byte[] data, int offset);

    int getMaxInstructionSize();
}
