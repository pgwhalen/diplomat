package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class OptionOpaque implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPTIONOPAQUE_NEW;
    private static final MethodHandle OPTIONOPAQUE_NEW_NONE;
    private static final MethodHandle OPTIONOPAQUE_RETURNS;
    private static final MethodHandle OPTIONOPAQUE_OPTION_ISIZE;
    private static final MethodHandle OPTIONOPAQUE_OPTION_USIZE;
    private static final MethodHandle OPTIONOPAQUE_OPTION_I32;
    private static final MethodHandle OPTIONOPAQUE_OPTION_U32;
    private static final MethodHandle OPTIONOPAQUE_NEW_STRUCT;
    private static final MethodHandle OPTIONOPAQUE_NEW_STRUCT_NONES;
    private static final MethodHandle OPTIONOPAQUE_RETURNS_NONE_SELF;
    private static final MethodHandle OPTIONOPAQUE_RETURNS_SOME_SELF;
    private static final MethodHandle OPTIONOPAQUE_ASSERT_INTEGER;
    private static final MethodHandle OPTIONOPAQUE_OPTION_OPAQUE_ARGUMENT;
    private static final MethodHandle OPTIONOPAQUE_ACCEPTS_BORROWING_OPTION_STRUCT;
    private static final MethodHandle OPTIONOPAQUE_RETURNS_OPTION_INPUT_STRUCT;
    static final StructLayout OPTIONOPAQUE_RETURNS_RESULT = MemoryLayout.structLayout(
            OptionStruct.LAYOUT.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );
    static final StructLayout OPTIONOPAQUE_OPTION_ISIZE_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );
    static final StructLayout OPTIONOPAQUE_OPTION_USIZE_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );
    static final StructLayout OPTIONOPAQUE_OPTION_I32_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(3)
        );
    static final StructLayout OPTIONOPAQUE_OPTION_U32_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(3)
        );

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
        OPTIONOPAQUE_NEW_NONE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_new_none").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_RETURNS = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_returns").orElseThrow(),
            FunctionDescriptor.of(OPTIONOPAQUE_RETURNS_RESULT)
        );
        OPTIONOPAQUE_OPTION_ISIZE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_option_isize").orElseThrow(),
            FunctionDescriptor.of(OPTIONOPAQUE_OPTION_ISIZE_RESULT, ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_OPTION_USIZE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_option_usize").orElseThrow(),
            FunctionDescriptor.of(OPTIONOPAQUE_OPTION_USIZE_RESULT, ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_OPTION_I32 = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_option_i32").orElseThrow(),
            FunctionDescriptor.of(OPTIONOPAQUE_OPTION_I32_RESULT, ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_OPTION_U32 = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_option_u32").orElseThrow(),
            FunctionDescriptor.of(OPTIONOPAQUE_OPTION_U32_RESULT, ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_NEW_STRUCT = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_new_struct").orElseThrow(),
            FunctionDescriptor.of(OptionStruct.LAYOUT)
        );
        OPTIONOPAQUE_NEW_STRUCT_NONES = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_new_struct_nones").orElseThrow(),
            FunctionDescriptor.of(OptionStruct.LAYOUT)
        );
        OPTIONOPAQUE_RETURNS_NONE_SELF = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_returns_none_self").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_RETURNS_SOME_SELF = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_returns_some_self").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_ASSERT_INTEGER = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_assert_integer").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
        OPTIONOPAQUE_OPTION_OPAQUE_ARGUMENT = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_option_opaque_argument").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_ACCEPTS_BORROWING_OPTION_STRUCT = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_accepts_borrowing_option_struct").orElseThrow(),
            FunctionDescriptor.ofVoid(BorrowingOptionStruct.LAYOUT)
        );
        OPTIONOPAQUE_RETURNS_OPTION_INPUT_STRUCT = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaque_returns_option_input_struct").orElseThrow(),
            FunctionDescriptor.of(OptionInputStruct.LAYOUT)
        );
    }

    final MemorySegment handle;

    OptionOpaque(MemorySegment handle) {
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

    public static Optional<OptionOpaque> new_(int i) {
        try {
            var resultAddr = (MemorySegment) OPTIONOPAQUE_NEW.invokeExact(i);
            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new OptionOpaque(resultAddr));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Optional<OptionOpaque> newNone() {
        try {
            var resultAddr = (MemorySegment) OPTIONOPAQUE_NEW_NONE.invokeExact();
            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new OptionOpaque(resultAddr));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Optional<OptionStruct> returns() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) OPTIONOPAQUE_RETURNS.invokeExact((SegmentAllocator) arena);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 32L);
            if (isOk) {
                return Optional.of(OptionStruct.fromNative(result.asSlice(0L, OptionStruct.LAYOUT.byteSize())));
            } else {
                return Optional.empty();
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static OptionStruct newStruct() {
        try (var arena = Arena.ofConfined()) {
            return OptionStruct.fromNative((MemorySegment) OPTIONOPAQUE_NEW_STRUCT.invokeExact((SegmentAllocator) arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static OptionStruct newStructNones() {
        try (var arena = Arena.ofConfined()) {
            return OptionStruct.fromNative((MemorySegment) OPTIONOPAQUE_NEW_STRUCT_NONES.invokeExact((SegmentAllocator) arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean optionOpaqueArgument(OptionOpaque arg) {
        try {
            MemorySegment argAddr = arg == null ? MemorySegment.NULL : arg.handle;
            return (boolean) OPTIONOPAQUE_OPTION_OPAQUE_ARGUMENT.invokeExact(argAddr);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void acceptsBorrowingOptionStruct(BorrowingOptionStruct arg) {
        try (var arena = Arena.ofConfined()) {
            OPTIONOPAQUE_ACCEPTS_BORROWING_OPTION_STRUCT.invokeExact(arg.toNative(arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static OptionInputStruct returnsOptionInputStruct() {
        try (var arena = Arena.ofConfined()) {
            return OptionInputStruct.fromNative((MemorySegment) OPTIONOPAQUE_RETURNS_OPTION_INPUT_STRUCT.invokeExact((SegmentAllocator) arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Optional<Long> optionIsize() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) OPTIONOPAQUE_OPTION_ISIZE.invokeExact((SegmentAllocator) arena, handle);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return Optional.of((long) result.get(ValueLayout.JAVA_LONG, 0L));
            } else {
                return Optional.empty();
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Optional<Long> optionUsize() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) OPTIONOPAQUE_OPTION_USIZE.invokeExact((SegmentAllocator) arena, handle);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return Optional.of((long) result.get(ValueLayout.JAVA_LONG, 0L));
            } else {
                return Optional.empty();
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Optional<Integer> optionI32() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) OPTIONOPAQUE_OPTION_I32.invokeExact((SegmentAllocator) arena, handle);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 4L);
            if (isOk) {
                return Optional.of((int) result.get(ValueLayout.JAVA_INT, 0L));
            } else {
                return Optional.empty();
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Optional<Integer> optionU32() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) OPTIONOPAQUE_OPTION_U32.invokeExact((SegmentAllocator) arena, handle);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 4L);
            if (isOk) {
                return Optional.of((int) result.get(ValueLayout.JAVA_INT, 0L));
            } else {
                return Optional.empty();
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Optional<OptionOpaque> returnsNoneSelf() {
        try {
            var resultAddr = (MemorySegment) OPTIONOPAQUE_RETURNS_NONE_SELF.invokeExact(handle);
            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new OptionOpaque(resultAddr));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Optional<OptionOpaque> returnsSomeSelf() {
        try {
            var resultAddr = (MemorySegment) OPTIONOPAQUE_RETURNS_SOME_SELF.invokeExact(handle);
            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new OptionOpaque(resultAddr));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void assertInteger(int i) {
        try {
            OPTIONOPAQUE_ASSERT_INTEGER.invokeExact(handle, i);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}