package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class Utf16Wrap implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle UTF16WRAP_FROM_UTF16;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("Utf16Wrap_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        UTF16WRAP_FROM_UTF16 = LINKER.downcallHandle(
            LIB.find("Utf16Wrap_from_utf16").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    final MemorySegment handle;

    Utf16Wrap(MemorySegment handle) {
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

    public static Utf16Wrap fromUtf16(String input) {
        try (var arena = Arena.ofConfined()) {
            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

            var inputSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, inputBytes);
            return new Utf16Wrap((MemorySegment) UTF16WRAP_FROM_UTF16.invokeExact(inputSeg, (long) inputBytes.length));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}