package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class RenamedOpaqueRefIterable implements AutoCloseable, Iterable<AttrOpaque1Renamed> {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_OPAQUEREFITERABLE_NEW;
    private static final MethodHandle NAMESPACE_OPAQUEREFITERABLE_ITER;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("namespace_OpaqueRefIterable_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_OPAQUEREFITERABLE_NEW = LINKER.downcallHandle(
            LIB.find("namespace_OpaqueRefIterable_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        NAMESPACE_OPAQUEREFITERABLE_ITER = LINKER.downcallHandle(
            LIB.find("namespace_OpaqueRefIterable_iter").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    RenamedOpaqueRefIterable(MemorySegment handle) {
        this.handle = handle;
    }

    public RenamedOpaqueRefIterable(long size) {
        try {
            this.handle = (MemorySegment) NAMESPACE_OPAQUEREFITERABLE_NEW.invokeExact(size);
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

    @Override
    public RenamedOpaqueRefIterator iterator() {
        try {
            return new RenamedOpaqueRefIterator((MemorySegment) NAMESPACE_OPAQUEREFITERABLE_ITER.invokeExact(handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}