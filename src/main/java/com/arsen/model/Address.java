package com.arsen.model;

public record Address(long value) implements Comparable<Address> {
    public static Address of(long value) {
        return new Address(value);
    }

    public Address add(long offset) {
        return new Address(value + offset);
    }

    public long distance(Address other) {
        return Math.abs(value - other.value);
    }

    @Override
    public int compareTo(Address other) {
        return Long.compare(value, other.value);
    }

    @Override
    public String toString() {
        return String.format("0x%016X", value);
    }
}
