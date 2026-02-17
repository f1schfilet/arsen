package com.arsen.core.analysis.graph;

import com.arsen.model.Address;
import com.arsen.model.disassembly.BasicBlock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ControlFlowGraph(Map<Address, BasicBlock> blocks, Map<Address, List<Address>> successors,
                               Map<Address, List<Address>> predecessors, Address entryBlock) {

    public BasicBlock getBlock(Address address) {
        return blocks.get(address);
    }

    public List<Address> getSuccessors(Address address) {
        return successors.getOrDefault(address, Collections.emptyList());
    }

    public List<Address> getPredecessors(Address address) {
        return predecessors.getOrDefault(address, Collections.emptyList());
    }

    public Set<Address> getAllBlockAddresses() {
        return blocks.keySet();
    }

    public int getBlockCount() {
        return blocks.size();
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }
}
