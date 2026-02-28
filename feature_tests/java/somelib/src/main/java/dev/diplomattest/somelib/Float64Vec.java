package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class Float64Vec implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle FLOAT64VEC_TO_STRING;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("Float64Vec_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        FLOAT64VEC_TO_STRING = LINKER.downcallHandle(
            LIB.find("Float64Vec_to_string").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    Float64Vec(MemorySegment handle) {
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

    public String toString() {
        var write = DiplomatLib.createWrite();
        try {
            FLOAT64VEC_TO_STRING.invokeExact(handle, write);
            return DiplomatLib.writeToString(write);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}