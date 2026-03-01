package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public enum RenamedAttrEnum {
    A(0),
    B(1),
    Renamed(2);

    private final int value;

    RenamedAttrEnum(int value) {
        this.value = value;
    }

    public int toNative() {
        return this.value;
    }

    public static RenamedAttrEnum fromNative(int value) {
        for (var variant : values()) {
            if (variant.value == value) {
                return variant;
            }
        }
        throw new IllegalArgumentException("Invalid native value for RenamedAttrEnum: " + value);
    }
}