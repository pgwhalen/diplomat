package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public enum OptionEnum {
    FOO(0),
    BAR(1),
    BAZ(2);

    private final int value;

    OptionEnum(int value) {
        this.value = value;
    }

    public int toNative() {
        return this.value;
    }

    public static OptionEnum fromNative(int value) {
        for (var variant : values()) {
            if (variant.value == value) {
                return variant;
            }
        }
        throw new IllegalArgumentException("Invalid native value for OptionEnum: " + value);
    }
}