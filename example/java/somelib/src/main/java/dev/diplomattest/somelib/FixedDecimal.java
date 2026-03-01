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
    private static final MethodHandle ICU4X_FIXEDDECIMAL_TO_STRING_MV1;
    static final StructLayout ICU4X_FIXEDDECIMAL_TO_STRING_MV1_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_BOOLEAN.withName("is_ok")
        );

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
        ICU4X_FIXEDDECIMAL_TO_STRING_MV1 = LINKER.downcallHandle(
            LIB.find("icu4x_FixedDecimal_to_string_mv1").orElseThrow(),
            FunctionDescriptor.of(ICU4X_FIXEDDECIMAL_TO_STRING_MV1_RESULT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    FixedDecimal(MemorySegment handle) {
        this.handle = handle;
    }

    public FixedDecimal(int v) {
        try {
            this.handle = (MemorySegment) ICU4X_FIXEDDECIMAL_NEW_MV1.invokeExact(v);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() {
        try {
            DESTROY.invokeExact(handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void multiplyPow10(short power) {
        try {
            ICU4X_FIXEDDECIMAL_MULTIPLY_POW10_MV1.invokeExact(handle, power);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String toString() {
        var write = DiplomatLib.createWrite();
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) ICU4X_FIXEDDECIMAL_TO_STRING_MV1.invokeExact((SegmentAllocator) arena, handle, write);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 0L);
            if (isOk) {
                return DiplomatLib.writeToString(write);
            } else {
                DiplomatLib.destroyWrite(write);
                throw new RuntimeException("Diplomat error");
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}