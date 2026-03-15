package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class OpaqueMutexedString implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPAQUEMUTEXEDSTRING_FROM_USIZE;
    private static final MethodHandle OPAQUEMUTEXEDSTRING_CHANGE;
    private static final MethodHandle OPAQUEMUTEXEDSTRING_BORROW;
    private static final MethodHandle OPAQUEMUTEXEDSTRING_BORROW_OTHER;
    private static final MethodHandle OPAQUEMUTEXEDSTRING_BORROW_SELF_OR_OTHER;
    private static final MethodHandle OPAQUEMUTEXEDSTRING_GET_LEN_AND_ADD;
    private static final MethodHandle OPAQUEMUTEXEDSTRING_WRAPPER;
    private static final MethodHandle OPAQUEMUTEXEDSTRING_TO_UNSIGNED_FROM_UNSIGNED;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueMutexedString_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPAQUEMUTEXEDSTRING_FROM_USIZE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueMutexedString_from_usize").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        OPAQUEMUTEXEDSTRING_CHANGE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueMutexedString_change").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        OPAQUEMUTEXEDSTRING_BORROW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueMutexedString_borrow").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        OPAQUEMUTEXEDSTRING_BORROW_OTHER = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueMutexedString_borrow_other").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        OPAQUEMUTEXEDSTRING_BORROW_SELF_OR_OTHER = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueMutexedString_borrow_self_or_other").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        OPAQUEMUTEXEDSTRING_GET_LEN_AND_ADD = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueMutexedString_get_len_and_add").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        OPAQUEMUTEXEDSTRING_WRAPPER = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueMutexedString_wrapper").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        OPAQUEMUTEXEDSTRING_TO_UNSIGNED_FROM_UNSIGNED = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OpaqueMutexedString_to_unsigned_from_unsigned").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.ADDRESS, ValueLayout.JAVA_SHORT)
        );
    }

    final MemorySegment handle;

    OpaqueMutexedString(MemorySegment handle) {
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

    public static OpaqueMutexedString fromUsize(long number) {
        try {
            return new OpaqueMutexedString((MemorySegment) OPAQUEMUTEXEDSTRING_FROM_USIZE.invokeExact(number));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static OpaqueMutexedString borrowOther(OpaqueMutexedString other) {
        try {
            return new OpaqueMutexedString((MemorySegment) OPAQUEMUTEXEDSTRING_BORROW_OTHER.invokeExact(other.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void change(long number) {
        try {
            OPAQUEMUTEXEDSTRING_CHANGE.invokeExact(handle, number);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public OpaqueMutexedString borrow() {
        try {
            return new OpaqueMutexedString((MemorySegment) OPAQUEMUTEXEDSTRING_BORROW.invokeExact(handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public OpaqueMutexedString borrowSelfOrOther(OpaqueMutexedString other) {
        try {
            return new OpaqueMutexedString((MemorySegment) OPAQUEMUTEXEDSTRING_BORROW_SELF_OR_OTHER.invokeExact(handle, other.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public long getLenAndAdd(long other) {
        try {
            return (long) OPAQUEMUTEXEDSTRING_GET_LEN_AND_ADD.invokeExact(handle, other);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Utf16Wrap wrapper() {
        try {
            return new Utf16Wrap((MemorySegment) OPAQUEMUTEXEDSTRING_WRAPPER.invokeExact(handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public short toUnsignedFromUnsigned(short input) {
        try {
            return (short) OPAQUEMUTEXEDSTRING_TO_UNSIGNED_FROM_UNSIGNED.invokeExact(handle, input);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}