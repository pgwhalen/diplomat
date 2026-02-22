package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class FixedDecimalFormatter implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;

    static {
        System.loadLibrary("diplomat_example");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("icu4x_FixedDecimalFormatter_destroy_mv1").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    FixedDecimalFormatter(MemorySegment handle) {
        this.handle = handle;
    }

    @Override
    public void close() {
        try { DESTROY.invokeExact(handle); }
        catch (Throwable e) { throw new RuntimeException(e); }
    }
}