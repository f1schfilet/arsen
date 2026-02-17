package com.arsen.model.disassembly;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Operand {
    OperandType type;
    String text;
    long value;
}
