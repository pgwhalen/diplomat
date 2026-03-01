package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class OpaqueThinVec implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
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

    @Override
    public void close() {
        try {
            DESTROY.invokeExact(handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public OpaqueThinIter iter() {
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

    public Optional<OpaqueThin> get(long idx) {
        try {
            var resultAddr = (MemorySegment) OPAQUETHINVEC_GET.invokeExact(handle, idx);
            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new OpaqueThin(resultAddr));
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