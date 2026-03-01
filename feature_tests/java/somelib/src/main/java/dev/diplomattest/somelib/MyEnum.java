package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public enum MyEnum {
    A(-2),
    B(-1),
    C(0),
    D(1),
    E(2),
    F(3);

    private final int value;

    MyEnum(int value) {
        this.value = value;
    }

    public int toNative() {
        return this.value;
    }

    public static MyEnum fromNative(int value) {
        for (var variant : values()) {
            if (variant.value == value) {
                return variant;
            }
        }
        throw new IllegalArgumentException("Invalid native value for MyEnum: " + value);
    }

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle MYENUM_INTO_VALUE;
    private static final MethodHandle MYENUM_GET_A;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        MYENUM_INTO_VALUE = LINKER.downcallHandle(
            LIB.find("MyEnum_into_value").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_BYTE, ValueLayout.JAVA_INT)
        );
        MYENUM_GET_A = LINKER.downcallHandle(
            LIB.find("MyEnum_get_a").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT)
        );
    }

    public static MyEnum getA() {
        try {
            return MyEnum.fromNative((int) MYENUM_GET_A.invokeExact());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public byte intoValue() {
        try {
            return (byte) MYENUM_INTO_VALUE.invokeExact(this.toNative());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}