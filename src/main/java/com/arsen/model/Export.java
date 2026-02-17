package com.arsen.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Export {
    String name;
    Address address;
    int ordinal;
}
