package com.arsen.core.analysis;

import com.arsen.model.Address;
import com.arsen.model.binary.BinaryFile;
import com.arsen.model.disassembly.CrossReference;
import com.arsen.model.disassembly.Function;
import com.arsen.model.disassembly.Instruction;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class AnalysisContext {
    private final BinaryFile binaryFile;
    private final Map<Address, Instruction> instructions;
    private final Map<Address, Function> functions;
    private final List<CrossReference> crossReferences;
    private final List<String> strings;

    public AnalysisContext(BinaryFile binaryFile) {
        this.binaryFile = binaryFile;
        this.instructions = new ConcurrentHashMap<>();
        this.functions = new ConcurrentHashMap<>();
        this.crossReferences = new CopyOnWriteArrayList<>();
        this.strings = new CopyOnWriteArrayList<>();
    }

    public void addInstruction(Instruction instruction) {
        instructions.put(instruction.getAddress(), instruction);
    }

    public void addFunction(Function function) {
        functions.put(function.getAddress(), function);
    }

    public void addCrossReference(CrossReference xref) {
        crossReferences.add(xref);
    }

    public void addString(String str) {
        strings.add(str);
    }

    public AnalysisResult buildResult() {
        return AnalysisResult.builder().instructions(instructions).functions(functions).crossReferences(crossReferences).strings(strings).build();
    }
}
