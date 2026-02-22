package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class FixedDecimal implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle ICU4X_FIXEDDECIMAL_NEW_MV1;
    private static final MethodHandle ICU4X_FIXEDDECIMAL_MULTIPLY_POW10_MV1;

    static {
        System.loadLibrary("diplomat_example");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("icu4x_FixedDecimal_destroy_mv1").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        ICU4X_FIXEDDECIMAL_NEW_MV1 = LINKER.downcallHandle(
            LIB.find("icu4x_FixedDecimal_new_mv1").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
        ICU4X_FIXEDDECIMAL_MULTIPLY_POW10_MV1 = LINKER.downcallHandle(
            LIB.find("icu4x_FixedDecimal_multiply_pow10_mv1").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_SHORT)
        );
    }

    final MemorySegment handle;

    FixedDecimal(MemorySegment handle) {
        this.handle = handle;
    }

    @Override
    public void close() {
        try { DESTROY.invokeExact(handle); }
        catch (Throwable e) { throw new RuntimeException(e); }
    }

    public static FixedDecimal new_(int v) {
        try { return new FixedDecimal((MemorySegment) ICU4X_FIXEDDECIMAL_NEW_MV1.invokeExact(v)); }

        catch (Throwable e) { throw new RuntimeException(e); }
    }

    public void multiplyPow10(short power) {
        try { ICU4X_FIXEDDECIMAL_MULTIPLY_POW10_MV1.invokeExact(handle, power); }

        catch (Throwable e) { throw new RuntimeException(e); }
    }
}