package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class RenamedOpaqueZSTIndexer implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_OPAQUEZSTINDEXER_NEW;
    private static final MethodHandle NAMESPACE_OPAQUEZSTINDEXER_INDEX;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_OpaqueZSTIndexer_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_OPAQUEZSTINDEXER_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_OpaqueZSTIndexer_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
        NAMESPACE_OPAQUEZSTINDEXER_INDEX = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_OpaqueZSTIndexer_index").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    final MemorySegment handle;

    RenamedOpaqueZSTIndexer(MemorySegment handle) {
        this.handle = handle;
    }

    public RenamedOpaqueZSTIndexer() {
        try {
            this.handle = (MemorySegment) NAMESPACE_OPAQUEZSTINDEXER_NEW.invokeExact();
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

    private RenamedOpaqueZSTIndexer getInternal(long idx) {
        try {
            var resultAddr = (MemorySegment) NAMESPACE_OPAQUEZSTINDEXER_INDEX.invokeExact(handle, idx);
            return resultAddr.equals(MemorySegment.NULL) ? null : new RenamedOpaqueZSTIndexer(resultAddr);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}