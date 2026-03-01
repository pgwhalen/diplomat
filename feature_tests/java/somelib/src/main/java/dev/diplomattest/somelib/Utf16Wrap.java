package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class Utf16Wrap implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle UTF16WRAP_FROM_UTF16;
    private static final MethodHandle UTF16WRAP_GET_DEBUG_STR;

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
        UTF16WRAP_GET_DEBUG_STR = LINKER.downcallHandle(
            LIB.find("Utf16Wrap_get_debug_str").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    Utf16Wrap(MemorySegment handle) {
        this.handle = handle;
    }

    public Utf16Wrap(String input) {
        try (var arena = Arena.ofConfined()) {
            char[] inputChars = input.toCharArray();

            var inputSeg = arena.allocateFrom(ValueLayout.JAVA_CHAR, inputChars);
            this.handle = (MemorySegment) UTF16WRAP_FROM_UTF16.invokeExact(inputSeg, (long) inputChars.length);
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

    public String getDebugStr() {
        var write = DiplomatLib.createWrite();
        try {
            UTF16WRAP_GET_DEBUG_STR.invokeExact(handle, write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}