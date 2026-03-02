package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class RenamedMyIterable implements AutoCloseable, Iterable<Byte> {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_MYITERABLE_NEW;
    private static final MethodHandle NAMESPACE_MYITERABLE_ITER;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("namespace_MyIterable_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_MYITERABLE_NEW = LINKER.downcallHandle(
            LIB.find("namespace_MyIterable_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        NAMESPACE_MYITERABLE_ITER = LINKER.downcallHandle(
            LIB.find("namespace_MyIterable_iter").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    RenamedMyIterable(MemorySegment handle) {
        this.handle = handle;
    }

    public RenamedMyIterable(byte[] x) {
        try (var arena = Arena.ofConfined()) {
            var xSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, x);
            this.handle = (MemorySegment) NAMESPACE_MYITERABLE_NEW.invokeExact(xSeg, (long) x.length);
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
    public RenamedMyIterator iterator() {
        try {
            return new RenamedMyIterator((MemorySegment) NAMESPACE_MYITERABLE_ITER.invokeExact(handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}