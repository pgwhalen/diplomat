package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class ResultOpaque extends RuntimeException implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle RESULTOPAQUE_NEW;
    private static final MethodHandle RESULTOPAQUE_NEW_FAILING_FOO;
    private static final MethodHandle RESULTOPAQUE_NEW_FAILING_BAR;
    private static final MethodHandle RESULTOPAQUE_NEW_FAILING_UNIT;
    private static final MethodHandle RESULTOPAQUE_NEW_FAILING_STRUCT;
    private static final MethodHandle RESULTOPAQUE_NEW_IN_ERR;
    private static final MethodHandle RESULTOPAQUE_NEW_INT;
    private static final MethodHandle RESULTOPAQUE_NEW_FAILING_INT;
    private static final MethodHandle RESULTOPAQUE_NEW_IN_ENUM_ERR;
    private static final MethodHandle RESULTOPAQUE_GIVE_SELF;
    private static final MethodHandle RESULTOPAQUE_TAKES_STR;
    private static final MethodHandle RESULTOPAQUE_STRINGIFY_ERROR;
    private static final MethodHandle RESULTOPAQUE_ASSERT_INTEGER;
    static final StructLayout RESULTOPAQUE_NEW_RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );
    static final StructLayout RESULTOPAQUE_NEW_FAILING_FOO_RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );
    static final StructLayout RESULTOPAQUE_NEW_FAILING_BAR_RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );
    static final StructLayout RESULTOPAQUE_NEW_FAILING_UNIT_RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );
    static final StructLayout RESULTOPAQUE_NEW_FAILING_STRUCT_RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );
    static final StructLayout RESULTOPAQUE_NEW_IN_ERR_RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );
    static final StructLayout RESULTOPAQUE_NEW_INT_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(3)
        );
    static final StructLayout RESULTOPAQUE_NEW_FAILING_INT_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(3)
        );
    static final StructLayout RESULTOPAQUE_NEW_IN_ENUM_ERR_RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );
    static final StructLayout RESULTOPAQUE_GIVE_SELF_RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );
    static final StructLayout RESULTOPAQUE_STRINGIFY_ERROR_RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("ResultOpaque_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        RESULTOPAQUE_NEW = LINKER.downcallHandle(
            LIB.find("ResultOpaque_new").orElseThrow(),
            FunctionDescriptor.of(RESULTOPAQUE_NEW_RESULT, ValueLayout.JAVA_INT)
        );
        RESULTOPAQUE_NEW_FAILING_FOO = LINKER.downcallHandle(
            LIB.find("ResultOpaque_new_failing_foo").orElseThrow(),
            FunctionDescriptor.of(RESULTOPAQUE_NEW_FAILING_FOO_RESULT)
        );
        RESULTOPAQUE_NEW_FAILING_BAR = LINKER.downcallHandle(
            LIB.find("ResultOpaque_new_failing_bar").orElseThrow(),
            FunctionDescriptor.of(RESULTOPAQUE_NEW_FAILING_BAR_RESULT)
        );
        RESULTOPAQUE_NEW_FAILING_UNIT = LINKER.downcallHandle(
            LIB.find("ResultOpaque_new_failing_unit").orElseThrow(),
            FunctionDescriptor.of(RESULTOPAQUE_NEW_FAILING_UNIT_RESULT)
        );
        RESULTOPAQUE_NEW_FAILING_STRUCT = LINKER.downcallHandle(
            LIB.find("ResultOpaque_new_failing_struct").orElseThrow(),
            FunctionDescriptor.of(RESULTOPAQUE_NEW_FAILING_STRUCT_RESULT, ValueLayout.JAVA_INT)
        );
        RESULTOPAQUE_NEW_IN_ERR = LINKER.downcallHandle(
            LIB.find("ResultOpaque_new_in_err").orElseThrow(),
            FunctionDescriptor.of(RESULTOPAQUE_NEW_IN_ERR_RESULT, ValueLayout.JAVA_INT)
        );
        RESULTOPAQUE_NEW_INT = LINKER.downcallHandle(
            LIB.find("ResultOpaque_new_int").orElseThrow(),
            FunctionDescriptor.of(RESULTOPAQUE_NEW_INT_RESULT, ValueLayout.JAVA_INT)
        );
        RESULTOPAQUE_NEW_FAILING_INT = LINKER.downcallHandle(
            LIB.find("ResultOpaque_new_failing_int").orElseThrow(),
            FunctionDescriptor.of(RESULTOPAQUE_NEW_FAILING_INT_RESULT, ValueLayout.JAVA_INT)
        );
        RESULTOPAQUE_NEW_IN_ENUM_ERR = LINKER.downcallHandle(
            LIB.find("ResultOpaque_new_in_enum_err").orElseThrow(),
            FunctionDescriptor.of(RESULTOPAQUE_NEW_IN_ENUM_ERR_RESULT, ValueLayout.JAVA_INT)
        );
        RESULTOPAQUE_GIVE_SELF = LINKER.downcallHandle(
            LIB.find("ResultOpaque_give_self").orElseThrow(),
            FunctionDescriptor.of(RESULTOPAQUE_GIVE_SELF_RESULT, ValueLayout.ADDRESS)
        );
        RESULTOPAQUE_TAKES_STR = LINKER.downcallHandle(
            LIB.find("ResultOpaque_takes_str").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        RESULTOPAQUE_STRINGIFY_ERROR = LINKER.downcallHandle(
            LIB.find("ResultOpaque_stringify_error").orElseThrow(),
            FunctionDescriptor.of(RESULTOPAQUE_STRINGIFY_ERROR_RESULT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        RESULTOPAQUE_ASSERT_INTEGER = LINKER.downcallHandle(
            LIB.find("ResultOpaque_assert_integer").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
    }

    final MemorySegment handle;

    ResultOpaque(MemorySegment handle) {
        super("ResultOpaque");
        this.handle = handle;
    }

    public ResultOpaque(int i) {
        super("ResultOpaque");
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) RESULTOPAQUE_NEW.invokeExact((SegmentAllocator) arena, i);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                this.handle = result.get(ValueLayout.ADDRESS, 0L);
            } else {
                throw new ErrorEnumException(ErrorEnum.fromNative((int) result.get(ValueLayout.JAVA_INT, 0L)));
            }
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

    public static ResultOpaque failingFoo() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) RESULTOPAQUE_NEW_FAILING_FOO.invokeExact((SegmentAllocator) arena);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return new ResultOpaque(result.get(ValueLayout.ADDRESS, 0L));
            } else {
                throw new ErrorEnumException(ErrorEnum.fromNative((int) result.get(ValueLayout.JAVA_INT, 0L)));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static ResultOpaque failingBar() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) RESULTOPAQUE_NEW_FAILING_BAR.invokeExact((SegmentAllocator) arena);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return new ResultOpaque(result.get(ValueLayout.ADDRESS, 0L));
            } else {
                throw new ErrorEnumException(ErrorEnum.fromNative((int) result.get(ValueLayout.JAVA_INT, 0L)));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static ResultOpaque newFailingUnit() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) RESULTOPAQUE_NEW_FAILING_UNIT.invokeExact((SegmentAllocator) arena);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return new ResultOpaque(result.get(ValueLayout.ADDRESS, 0L));
            } else {
                throw new RuntimeException("Diplomat error");
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static ResultOpaque failingStruct(int i) {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) RESULTOPAQUE_NEW_FAILING_STRUCT.invokeExact((SegmentAllocator) arena, i);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return new ResultOpaque(result.get(ValueLayout.ADDRESS, 0L));
            } else {
                throw ErrorStruct.fromNative(result.asSlice(0L, ErrorStruct.LAYOUT.byteSize()));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void newInErr(int i) {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) RESULTOPAQUE_NEW_IN_ERR.invokeExact((SegmentAllocator) arena, i);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return;
            } else {
                throw new ResultOpaque(result.get(ValueLayout.ADDRESS, 0L));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int newInt(int i) {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) RESULTOPAQUE_NEW_INT.invokeExact((SegmentAllocator) arena, i);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 4L);
            if (isOk) {
                return (int) result.get(ValueLayout.JAVA_INT, 0L);
            } else {
                throw new RuntimeException("Diplomat error");
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void newFailingInt(int i) {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) RESULTOPAQUE_NEW_FAILING_INT.invokeExact((SegmentAllocator) arena, i);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 4L);
            if (isOk) {
                return;
            } else {
                throw new RuntimeException("Diplomat error: " + (int) result.get(ValueLayout.JAVA_INT, 0L));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static ErrorEnum newInEnumErr(int i) {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) RESULTOPAQUE_NEW_IN_ENUM_ERR.invokeExact((SegmentAllocator) arena, i);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return ErrorEnum.fromNative((int) result.get(ValueLayout.JAVA_INT, 0L));
            } else {
                throw new ResultOpaque(result.get(ValueLayout.ADDRESS, 0L));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void giveSelf() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) RESULTOPAQUE_GIVE_SELF.invokeExact((SegmentAllocator) arena, handle);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return;
            } else {
                throw new ResultOpaque(result.get(ValueLayout.ADDRESS, 0L));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * When we take &str, the return type becomes a Result
     * Test that this interacts gracefully with returning a reference type
     */
    public ResultOpaque takesStr(String v) {
        try (var arena = Arena.ofConfined()) {
            byte[] vBytes = v.getBytes(StandardCharsets.UTF_8);

            var vSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, vBytes);
            return new ResultOpaque((MemorySegment) RESULTOPAQUE_TAKES_STR.invokeExact(handle, vSeg, (long) vBytes.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String stringifyError() {
        var write = DiplomatLib.createWrite();
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) RESULTOPAQUE_STRINGIFY_ERROR.invokeExact((SegmentAllocator) arena, handle, write);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return DiplomatLib.writeToString(write);
            } else {
                DiplomatLib.destroyWrite(write);
                throw new ResultOpaque(result.get(ValueLayout.ADDRESS, 0L));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void assertInteger(int i) {
        try {
            RESULTOPAQUE_ASSERT_INTEGER.invokeExact(handle, i);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}