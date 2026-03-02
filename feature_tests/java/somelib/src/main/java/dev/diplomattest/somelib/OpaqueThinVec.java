package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Iterator;

public class OpaqueThinVec implements AutoCloseable, Iterable<OpaqueThin> {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPAQUETHINVEC_CREATE;
    private static final MethodHandle OPAQUETHINVEC_ITER;
    private static final MethodHandle OPAQUETHINVEC_LEN;
    private static final MethodHandle OPAQUETHINVEC_GET;
    private static final MethodHandle OPAQUETHINVEC_FIRST;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("OpaqueThinVec_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPAQUETHINVEC_CREATE = LINKER.downcallHandle(
            LIB.find("OpaqueThinVec_create").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        OPAQUETHINVEC_ITER = LINKER.downcallHandle(
            LIB.find("OpaqueThinVec_iter").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        OPAQUETHINVEC_LEN = LINKER.downcallHandle(
            LIB.find("OpaqueThinVec_len").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
        );
        OPAQUETHINVEC_GET = LINKER.downcallHandle(
            LIB.find("OpaqueThinVec_get").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        OPAQUETHINVEC_FIRST = LINKER.downcallHandle(
            LIB.find("OpaqueThinVec_first").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    OpaqueThinVec(MemorySegment handle) {
        this.handle = handle;
    }

    public OpaqueThinVec(int[] a, float[] b, String c) {
        try (var arena = Arena.ofConfined()) {
            byte[] cBytes = c.getBytes(StandardCharsets.UTF_8);

            var cSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, cBytes);
            var aSeg = arena.allocateFrom(ValueLayout.JAVA_INT, a);
            var bSeg = arena.allocateFrom(ValueLayout.JAVA_FLOAT, b);
            this.handle = (MemorySegment) OPAQUETHINVEC_CREATE.invokeExact(aSeg, (long) a.length, bSeg, (long) b.length, cSeg, (long) cBytes.length);
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
    public OpaqueThinIter iterator() {
        try {
            return new OpaqueThinIter((MemorySegment) OPAQUETHINVEC_ITER.invokeExact(handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public long len() {
        try {
            return (long) OPAQUETHINVEC_LEN.invokeExact(handle);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private OpaqueThin getInternal(long idx) {
        try {
            var resultAddr = (MemorySegment) OPAQUETHINVEC_GET.invokeExact(handle, idx);
            return resultAddr.equals(MemorySegment.NULL) ? null : new OpaqueThin(resultAddr);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Optional<OpaqueThin> first() {
        try {
            var resultAddr = (MemorySegment) OPAQUETHINVEC_FIRST.invokeExact(handle);
            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new OpaqueThin(resultAddr));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}