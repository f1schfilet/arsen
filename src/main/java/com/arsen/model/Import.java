package com.arsen.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Import {
    String library;
    String name;
    Address address;
    int ordinal;
}
