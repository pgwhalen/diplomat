package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class RenamedOpaqueIterable implements AutoCloseable, Iterable<AttrOpaque1Renamed> {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_OPAQUEITERABLE_NEW;
    private static final MethodHandle NAMESPACE_OPAQUEITERABLE_ITER;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("namespace_OpaqueIterable_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_OPAQUEITERABLE_NEW = LINKER.downcallHandle(
            LIB.find("namespace_OpaqueIterable_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        NAMESPACE_OPAQUEITERABLE_ITER = LINKER.downcallHandle(
            LIB.find("namespace_OpaqueIterable_iter").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    RenamedOpaqueIterable(MemorySegment handle) {
        this.handle = handle;
    }

    public RenamedOpaqueIterable(long size) {
        try {
            this.handle = (MemorySegment) NAMESPACE_OPAQUEITERABLE_NEW.invokeExact(size);
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
    public RenamedOpaqueIterator iterator() {
        try {
            return new RenamedOpaqueIterator((MemorySegment) NAMESPACE_OPAQUEITERABLE_ITER.invokeExact(handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}