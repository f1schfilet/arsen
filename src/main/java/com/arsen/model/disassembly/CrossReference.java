package com.arsen.model.disassembly;

import com.arsen.model.Address;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CrossReference {
    Address from;
    Address to;
    XRefType type;

    public enum XRefType {
        CALL, JUMP, DATA_READ, DATA_WRITE
    }
}
