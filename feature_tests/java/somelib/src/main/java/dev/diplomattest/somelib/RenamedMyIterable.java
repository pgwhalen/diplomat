package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class RenamedMyIterable implements AutoCloseable, Iterable<Byte> {

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_MYITERABLE_NEW;
    private static final MethodHandle NAMESPACE_MYITERABLE_ITER;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_MyIterable_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_MYITERABLE_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_MyIterable_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        NAMESPACE_MYITERABLE_ITER = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_MyIterable_iter").orElseThrow(),
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
            var xSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(xSlice, 0L, xSeg);
            DiplomatLib.VH_SV_LEN.set(xSlice, 0L, (long) x.length);
            this.handle = (MemorySegment) NAMESPACE_MYITERABLE_NEW.invokeExact(xSlice);
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