package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public enum DefaultEnum {
    A(0),
    B(1);

    private final int value;

    DefaultEnum(int value) {
        this.value = value;
    }

    public int toNative() {
        return this.value;
    }

    public static DefaultEnum fromNative(int value) {
        for (var variant : values()) {
            if (variant.value == value) {
                return variant;
            }
        }
        throw new IllegalArgumentException("Invalid native value for DefaultEnum: " + value);
    }
    private static final MethodHandle DEFAULTENUM_NEW;

    static {
        DEFAULTENUM_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("DefaultEnum_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT)
        );
    }

    public static DefaultEnum new_() {
        try {
            return DefaultEnum.fromNative((int) DEFAULTENUM_NEW.invokeExact());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}