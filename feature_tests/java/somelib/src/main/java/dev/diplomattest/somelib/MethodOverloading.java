package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class MethodOverloading implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle METHODOVERLOADING_FROM_INT32;
    private static final MethodHandle METHODOVERLOADING_FROM_INT64;
    private static final MethodHandle METHODOVERLOADING_FROM_UINT32;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MethodOverloading_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        METHODOVERLOADING_FROM_INT32 = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MethodOverloading_from_int32").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
        METHODOVERLOADING_FROM_INT64 = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MethodOverloading_from_int64").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        METHODOVERLOADING_FROM_UINT32 = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MethodOverloading_from_uint32").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
    }

    final MemorySegment handle;

    MethodOverloading(MemorySegment handle) {
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

    public static MethodOverloading from(int v) {
        try {
            return new MethodOverloading((MemorySegment) METHODOVERLOADING_FROM_INT32.invokeExact(v));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static MethodOverloading from(long v) {
        try {
            return new MethodOverloading((MemorySegment) METHODOVERLOADING_FROM_INT64.invokeExact(v));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static MethodOverloading fromUint32(int v) {
        try {
            return new MethodOverloading((MemorySegment) METHODOVERLOADING_FROM_UINT32.invokeExact(v));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}