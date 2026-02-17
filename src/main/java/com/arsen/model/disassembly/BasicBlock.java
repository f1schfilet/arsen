package com.arsen.model.disassembly;

import com.arsen.model.Address;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class BasicBlock {
    Address startAddress;
    Address endAddress;

    @Singular
    List<Instruction> instructions;

    @Singular
    List<Address> successors;

    @Singular
    List<Address> predecessors;
}
