package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class OpaqueThinIter implements AutoCloseable, Iterator<OpaqueThin> {

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPAQUETHINITER_NEXT;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueThinIter_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPAQUETHINITER_NEXT = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueThinIter_next").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;
    private OpaqueThin nextVal;

    OpaqueThinIter(MemorySegment handle) {
        this.handle = handle;
        this.nextVal = nextInternal();
    }

    @Override
    public void close() {
        try {
            DESTROY.invokeExact(handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private OpaqueThin nextInternal() {
        try {
            var resultAddr = (MemorySegment) OPAQUETHINITER_NEXT.invokeExact(handle);
            return resultAddr.equals(MemorySegment.NULL) ? null : new OpaqueThin(resultAddr);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean hasNext() {
        return nextVal != null;
    }

    @Override
    public OpaqueThin next() {
        OpaqueThin returnVal = nextVal;
        if (returnVal == null) {
            throw new NoSuchElementException();
        }
        nextVal = nextInternal();
        return returnVal;
    }
}