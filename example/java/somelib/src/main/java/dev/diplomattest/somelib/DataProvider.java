package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class DataProvider implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle ICU4X_DATAPROVIDER_NEW_STATIC_MV1;

    static {
        System.loadLibrary("diplomat_example");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("icu4x_DataProvider_destroy_mv1").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        ICU4X_DATAPROVIDER_NEW_STATIC_MV1 = LINKER.downcallHandle(
            LIB.find("icu4x_DataProvider_new_static_mv1").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    DataProvider(MemorySegment handle) {
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

    public static DataProvider newStatic() {
        try {
            return new DataProvider((MemorySegment) ICU4X_DATAPROVIDER_NEW_STATIC_MV1.invokeExact());
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}