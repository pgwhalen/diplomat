package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public enum ContiguousEnum {
    C(0),
    D(1),
    E(2),
    F(3);

    private final int value;

    ContiguousEnum(int value) {
        this.value = value;
    }

    public int toNative() {
        return this.value;
    }

    public static ContiguousEnum fromNative(int value) {
        for (var variant : values()) {
            if (variant.value == value) {
                return variant;
            }
        }
        throw new IllegalArgumentException("Invalid native value for ContiguousEnum: " + value);
    }
}