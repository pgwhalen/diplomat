package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class Float64Vec implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle FLOAT64VEC_NEW_BOOL;
    private static final MethodHandle FLOAT64VEC_NEW_I16;
    private static final MethodHandle FLOAT64VEC_NEW_U16;
    private static final MethodHandle FLOAT64VEC_NEW_ISIZE;
    private static final MethodHandle FLOAT64VEC_NEW_USIZE;
    private static final MethodHandle FLOAT64VEC_NEW_F64_BE_BYTES;
    private static final MethodHandle FLOAT64VEC_FILL_SLICE;
    private static final MethodHandle FLOAT64VEC_SET_VALUE;
    private static final MethodHandle FLOAT64VEC_TO_STRING;
    private static final MethodHandle FLOAT64VEC_GET;
    static final StructLayout FLOAT64VEC_GET_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_DOUBLE.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("Float64Vec_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        FLOAT64VEC_NEW_BOOL = LINKER.downcallHandle(
            LIB.find("Float64Vec_new_bool").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        FLOAT64VEC_NEW_I16 = LINKER.downcallHandle(
            LIB.find("Float64Vec_new_i16").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        FLOAT64VEC_NEW_U16 = LINKER.downcallHandle(
            LIB.find("Float64Vec_new_u16").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        FLOAT64VEC_NEW_ISIZE = LINKER.downcallHandle(
            LIB.find("Float64Vec_new_isize").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        FLOAT64VEC_NEW_USIZE = LINKER.downcallHandle(
            LIB.find("Float64Vec_new_usize").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        FLOAT64VEC_NEW_F64_BE_BYTES = LINKER.downcallHandle(
            LIB.find("Float64Vec_new_f64_be_bytes").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        FLOAT64VEC_FILL_SLICE = LINKER.downcallHandle(
            LIB.find("Float64Vec_fill_slice").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        FLOAT64VEC_SET_VALUE = LINKER.downcallHandle(
            LIB.find("Float64Vec_set_value").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        FLOAT64VEC_TO_STRING = LINKER.downcallHandle(
            LIB.find("Float64Vec_to_string").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        FLOAT64VEC_GET = LINKER.downcallHandle(
            LIB.find("Float64Vec_get").orElseThrow(),
            FunctionDescriptor.of(FLOAT64VEC_GET_RESULT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    final MemorySegment handle;

    Float64Vec(MemorySegment handle) {
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

    public static Float64Vec bool(boolean[] v) {
        try (var arena = Arena.ofConfined()) {
            byte[] vBytes = new byte[v.length];
            for (int i = 0; i < v.length; i++) vBytes[i] = v[i] ? (byte) 1 : (byte) 0;
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, vBytes);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_BOOL.invokeExact(vSeg, (long) v.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Float64Vec i16(short[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_SHORT, v);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_I16.invokeExact(vSeg, (long) v.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Float64Vec u16(short[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_SHORT, v);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_U16.invokeExact(vSeg, (long) v.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Float64Vec isize(long[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_LONG, v);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_ISIZE.invokeExact(vSeg, (long) v.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Float64Vec usize(long[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_LONG, v);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_USIZE.invokeExact(vSeg, (long) v.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Float64Vec f64BeBytes(byte[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, v);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_F64_BE_BYTES.invokeExact(vSeg, (long) v.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void fillSlice(double[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, v);
            FLOAT64VEC_FILL_SLICE.invokeExact(handle, vSeg, (long) v.length);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setValue(double[] newSlice) {
        try (var arena = Arena.ofConfined()) {
            var newSliceSeg = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, newSlice);
            FLOAT64VEC_SET_VALUE.invokeExact(handle, newSliceSeg, (long) newSlice.length);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String toString() {
        var write = DiplomatLib.createWrite();
        try {
            FLOAT64VEC_TO_STRING.invokeExact(handle, write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private Double getInternal(long i) {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) FLOAT64VEC_GET.invokeExact((SegmentAllocator) arena, handle, i);
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

    public Double get(long index) {
        Double returnVal = getInternal(index);
        if (returnVal == null) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds.");
        }
        return returnVal;
    }
}