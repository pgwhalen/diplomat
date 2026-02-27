package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class MyOpaqueEnum implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle MYOPAQUEENUM_NEW;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("MyOpaqueEnum_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        MYOPAQUEENUM_NEW = LINKER.downcallHandle(
            LIB.find("MyOpaqueEnum_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    MyOpaqueEnum(MemorySegment handle) {
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

    public static MyOpaqueEnum new_() {
        try {
            return new MyOpaqueEnum((MemorySegment) MYOPAQUEENUM_NEW.invokeExact());
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}