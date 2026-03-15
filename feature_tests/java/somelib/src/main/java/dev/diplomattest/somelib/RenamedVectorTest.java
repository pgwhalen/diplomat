package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class RenamedVectorTest implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_VECTORTEST_NEW;
    private static final MethodHandle NAMESPACE_VECTORTEST_LEN;
    private static final MethodHandle NAMESPACE_VECTORTEST_GET;
    private static final MethodHandle NAMESPACE_VECTORTEST_PUSH;
    static final StructLayout NAMESPACE_VECTORTEST_GET_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_DOUBLE.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_VectorTest_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_VECTORTEST_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_VectorTest_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
        NAMESPACE_VECTORTEST_LEN = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_VectorTest_len").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
        );
        NAMESPACE_VECTORTEST_GET = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_VectorTest_get").orElseThrow(),
            FunctionDescriptor.of(NAMESPACE_VECTORTEST_GET_RESULT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        NAMESPACE_VECTORTEST_PUSH = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_VectorTest_push").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE)
        );
    }

    final MemorySegment handle;

    RenamedVectorTest(MemorySegment handle) {
        this.handle = handle;
    }

    public RenamedVectorTest() {
        try {
            this.handle = (MemorySegment) NAMESPACE_VECTORTEST_NEW.invokeExact();
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

    public long len() {
        try {
            return (long) NAMESPACE_VECTORTEST_LEN.invokeExact(handle);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private Double getInternal(long idx) {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) NAMESPACE_VECTORTEST_GET.invokeExact((SegmentAllocator) arena, handle, idx);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return (double) result.get(ValueLayout.JAVA_DOUBLE, 0L);
            } else {
                return null;
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void push(double value) {
        try {
            NAMESPACE_VECTORTEST_PUSH.invokeExact(handle, value);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Double get(long index) {
        Double returnVal = getInternal(index);
        if (returnVal == null) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds.");
        }
        return returnVal;
    }
}