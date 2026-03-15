package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class RenamedComparable implements AutoCloseable, Comparable<RenamedComparable> {

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_COMPARABLE_NEW;
    private static final MethodHandle NAMESPACE_COMPARABLE_CMP;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_Comparable_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_COMPARABLE_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_Comparable_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_BYTE)
        );
        NAMESPACE_COMPARABLE_CMP = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_Comparable_cmp").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_BYTE, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    RenamedComparable(MemorySegment handle) {
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

    public static RenamedComparable new_(byte int_) {
        try {
            return new RenamedComparable((MemorySegment) NAMESPACE_COMPARABLE_NEW.invokeExact(int_));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int compareTo(RenamedComparable other) {
        try {
            return (int)(byte) NAMESPACE_COMPARABLE_CMP.invokeExact(handle, other.handle);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}