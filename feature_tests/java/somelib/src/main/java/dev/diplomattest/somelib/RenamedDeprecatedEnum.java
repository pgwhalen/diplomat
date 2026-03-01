package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public enum RenamedDeprecatedEnum {
    A(0);

    private final int value;

    RenamedDeprecatedEnum(int value) {
        this.value = value;
    }

    public int toNative() {
        return this.value;
    }

    public static RenamedDeprecatedEnum fromNative(int value) {
        for (var variant : values()) {
            if (variant.value == value) {
                return variant;
            }
        }
        throw new IllegalArgumentException("Invalid native value for RenamedDeprecatedEnum: " + value);
    }
}