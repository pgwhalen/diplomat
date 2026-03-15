package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RenamedOpaqueRefIterator implements AutoCloseable, Iterator<AttrOpaque1Renamed> {

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_OPAQUEREFITERATOR_NEXT;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_OpaqueRefIterator_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_OPAQUEREFITERATOR_NEXT = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_OpaqueRefIterator_next").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;
    private AttrOpaque1Renamed nextVal;

    RenamedOpaqueRefIterator(MemorySegment handle) {
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

    private AttrOpaque1Renamed nextInternal() {
        try {
            var resultAddr = (MemorySegment) NAMESPACE_OPAQUEREFITERATOR_NEXT.invokeExact(handle);
            return resultAddr.equals(MemorySegment.NULL) ? null : new AttrOpaque1Renamed(resultAddr);
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
    public AttrOpaque1Renamed next() {
        AttrOpaque1Renamed returnVal = nextVal;
        if (returnVal == null) {
            throw new NoSuchElementException();
        }
        nextVal = nextInternal();
        return returnVal;
    }
}