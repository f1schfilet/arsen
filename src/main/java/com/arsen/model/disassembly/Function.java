package com.arsen.model.disassembly;

import com.arsen.model.Address;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class Function {
    Address address;
    String name;
    long size;

    @Singular
    List<Address> callers;

    @Singular
    List<Address> callees;

    @Singular
    List<BasicBlock> basicBlocks;
}
