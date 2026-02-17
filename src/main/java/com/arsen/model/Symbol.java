package com.arsen.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Symbol {
    Address address;
    String name;
    SymbolType type;

    public enum SymbolType {
        FUNCTION, DATA, IMPORT, EXPORT, LABEL
    }
}
