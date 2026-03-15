package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class OpaqueThin implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPAQUETHIN_A;
    private static final MethodHandle OPAQUETHIN_B;
    private static final MethodHandle OPAQUETHIN_C;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueThin_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPAQUETHIN_A = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueThin_a").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        );
        OPAQUETHIN_B = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueThin_b").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.ADDRESS)
        );
        OPAQUETHIN_C = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueThin_c").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    OpaqueThin(MemorySegment handle) {
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

    public int a() {
        try {
            return (int) OPAQUETHIN_A.invokeExact(handle);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public float b() {
        try {
            return (float) OPAQUETHIN_B.invokeExact(handle);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String c() {
        var write = DiplomatLib.createWrite();
        try {
            OPAQUETHIN_C.invokeExact(handle, write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}