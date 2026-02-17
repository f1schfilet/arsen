package com.arsen.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Section {
    String name;
    Address virtualAddress;
    long virtualSize;
    Address rawAddress;
    long rawSize;
    int flags;
    byte[] data;

    public boolean isExecutable() {
        return (flags & 0x20000000) != 0;
    }

    public boolean isReadable() {
        return (flags & 0x40000000) != 0;
    }

    public boolean isWritable() {
        return (flags & 0x80000000) != 0;
    }

    public boolean containsAddress(Address address) {
        long addr = address.value();
        long start = virtualAddress.value();
        long end = start + virtualSize;
        return addr >= start && addr < end;
    }
}
