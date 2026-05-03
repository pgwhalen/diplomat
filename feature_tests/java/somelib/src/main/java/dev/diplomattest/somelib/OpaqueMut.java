package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class OpaqueMut implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPAQUEMUT_NEW;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueMut_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPAQUEMUT_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueMut_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    OpaqueMut(MemorySegment handle) {
        this.handle = handle;
    }

    public OpaqueMut() {
        try {
            this.handle = (MemorySegment) OPAQUEMUT_NEW.invokeExact();
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
}