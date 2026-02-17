package com.arsen.core.analysis;

import com.arsen.model.Address;
import com.arsen.model.disassembly.CrossReference;
import com.arsen.model.disassembly.Function;
import com.arsen.model.disassembly.Instruction;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class AnalysisResult {
    Map<Address, Instruction> instructions;
    Map<Address, Function> functions;
    List<CrossReference> crossReferences;
    List<String> strings;
}
