package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class OpaqueThinIter implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPAQUETHINITER_NEXT;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("OpaqueThinIter_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPAQUETHINITER_NEXT = LINKER.downcallHandle(
            LIB.find("OpaqueThinIter_next").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    OpaqueThinIter(MemorySegment handle) {
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

    public Optional<OpaqueThin> next() {
        try {
            var resultAddr = (MemorySegment) OPAQUETHINITER_NEXT.invokeExact(handle);
            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new OpaqueThin(resultAddr));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}