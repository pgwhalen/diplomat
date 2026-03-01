package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public enum FixedDecimalGroupingStrategy {
    AUTO(0),
    NEVER(1),
    ALWAYS(2),
    MIN2(3);

    private final int value;

    FixedDecimalGroupingStrategy(int value) {
        this.value = value;
    }

    public int toNative() {
        return this.value;
    }

    public static FixedDecimalGroupingStrategy fromNative(int value) {
        for (var variant : values()) {
            if (variant.value == value) {
                return variant;
            }
        }
        throw new IllegalArgumentException("Invalid native value for FixedDecimalGroupingStrategy: " + value);
    }
}