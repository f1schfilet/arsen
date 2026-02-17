package com.arsen.core.analysis.graph;

import com.arsen.model.Address;
import com.arsen.model.disassembly.BasicBlock;
import com.arsen.model.disassembly.Function;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ControlFlowGraphBuilder {

    public ControlFlowGraph build(Function function) {
        if (function == null || function.getBasicBlocks() == null || function.getBasicBlocks().isEmpty()) {
            return new ControlFlowGraph(new HashMap<>(), new HashMap<>(), new HashMap<>(), null);
        }

        List<BasicBlock> blocks = new ArrayList<>(function.getBasicBlocks());
        blocks.sort(Comparator.comparing(BasicBlock::getStartAddress));

        Map<Address, BasicBlock> blockMap = new HashMap<>();
        Map<Address, List<Address>> successors = new HashMap<>();
        Map<Address, List<Address>> predecessors = new HashMap<>();

        for (BasicBlock block : blocks) {
            Address start = block.getStartAddress();
            blockMap.put(start, block);

            List<Address> succList = new ArrayList<>(block.getSuccessors());
            succList.sort(Address::compareTo);
            successors.put(start, succList);

            List<Address> predList = new ArrayList<>(block.getPredecessors());
            predList.sort(Address::compareTo);
            predecessors.put(start, predList);
        }

        Address entryBlock = blocks.isEmpty() ? null : blocks.get(0).getStartAddress();

        log.debug("Built CFG for function {} with {} blocks", function.getName(), blockMap.size());

        return new ControlFlowGraph(blockMap, successors, predecessors, entryBlock);
    }
}
