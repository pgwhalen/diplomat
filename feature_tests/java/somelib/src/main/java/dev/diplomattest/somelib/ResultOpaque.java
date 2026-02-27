package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class ResultOpaque implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle RESULTOPAQUE_TAKES_STR;
    private static final MethodHandle RESULTOPAQUE_ASSERT_INTEGER;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("ResultOpaque_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        RESULTOPAQUE_TAKES_STR = LINKER.downcallHandle(
            LIB.find("ResultOpaque_takes_str").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        RESULTOPAQUE_ASSERT_INTEGER = LINKER.downcallHandle(
            LIB.find("ResultOpaque_assert_integer").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
    }

    final MemorySegment handle;

    ResultOpaque(MemorySegment handle) {
        this.handle = handle;
    }

    @Override
    public void close() {
        try {
            DESTROY.invokeExact(handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public ResultOpaque takesStr(String v) {
        try (var arena = Arena.ofConfined()) {
            byte[] vBytes = v.getBytes(StandardCharsets.UTF_8);

            var vSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, vBytes);
            return new ResultOpaque((MemorySegment) RESULTOPAQUE_TAKES_STR.invokeExact(handle, vSeg, (long) vBytes.length));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void assertInteger(int i) {
        try {
            RESULTOPAQUE_ASSERT_INTEGER.invokeExact(handle, i);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}