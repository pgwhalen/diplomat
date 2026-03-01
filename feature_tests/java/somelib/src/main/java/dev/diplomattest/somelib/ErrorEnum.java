package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public enum ErrorEnum {
    FOO(0),
    BAR(1);

    private final int value;

    ErrorEnum(int value) {
        this.value = value;
    }

    public int toNative() {
        return this.value;
    }

    public static ErrorEnum fromNative(int value) {
        for (var variant : values()) {
            if (variant.value == value) {
                return variant;
            }
        }
        throw new IllegalArgumentException("Invalid native value for ErrorEnum: " + value);
    }
}

class ErrorEnumException extends RuntimeException {
    private final ErrorEnum value;

    ErrorEnumException(ErrorEnum value) {
        super("ErrorEnum error: " + value);
        this.value = value;
    }

    public ErrorEnum getValue() {
        return value;
    }
}