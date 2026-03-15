package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class Float64Vec implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle FLOAT64VEC_NEW_BOOL;
    private static final MethodHandle FLOAT64VEC_NEW_I16;
    private static final MethodHandle FLOAT64VEC_NEW_U16;
    private static final MethodHandle FLOAT64VEC_NEW_ISIZE;
    private static final MethodHandle FLOAT64VEC_NEW_USIZE;
    private static final MethodHandle FLOAT64VEC_NEW_F64_BE_BYTES;
    private static final MethodHandle FLOAT64VEC_NEW_FROM_OWNED;
    private static final MethodHandle FLOAT64VEC_AS_SLICE;
    private static final MethodHandle FLOAT64VEC_FILL_SLICE;
    private static final MethodHandle FLOAT64VEC_SET_VALUE;
    private static final MethodHandle FLOAT64VEC_TO_STRING;
    private static final MethodHandle FLOAT64VEC_BORROW;
    private static final MethodHandle FLOAT64VEC_GET;
    static final StructLayout FLOAT64VEC_GET_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_DOUBLE.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        FLOAT64VEC_NEW_BOOL = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_new_bool").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        FLOAT64VEC_NEW_I16 = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_new_i16").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        FLOAT64VEC_NEW_U16 = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_new_u16").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        FLOAT64VEC_NEW_ISIZE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_new_isize").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        FLOAT64VEC_NEW_USIZE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_new_usize").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        FLOAT64VEC_NEW_F64_BE_BYTES = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_new_f64_be_bytes").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        FLOAT64VEC_NEW_FROM_OWNED = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_new_from_owned").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        FLOAT64VEC_AS_SLICE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_as_slice").orElseThrow(),
            FunctionDescriptor.of(DiplomatLib.DIPLOMAT_STRING_VIEW, ValueLayout.ADDRESS)
        );
        FLOAT64VEC_FILL_SLICE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_fill_slice").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        FLOAT64VEC_SET_VALUE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_set_value").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        FLOAT64VEC_TO_STRING = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_to_string").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        FLOAT64VEC_BORROW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_borrow").orElseThrow(),
            FunctionDescriptor.of(DiplomatLib.DIPLOMAT_STRING_VIEW, ValueLayout.ADDRESS)
        );
        FLOAT64VEC_GET = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Float64Vec_get").orElseThrow(),
            FunctionDescriptor.of(FLOAT64VEC_GET_RESULT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    final MemorySegment handle;

    Float64Vec(MemorySegment handle) {
        this.handle = handle;
    }

    public Float64Vec(double[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSrc = MemorySegment.ofArray(v);
            var vSeg = DiplomatLib.diplomatAlloc((long) v.length * 8L, 8L).reinterpret((long) v.length * 8L);
            MemorySegment.copy(vSrc, 0, vSeg, 0, (long) v.length * 8L);
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) v.length);
            this.handle = (MemorySegment) FLOAT64VEC_NEW_FROM_OWNED.invokeExact(vSlice);
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

    public static Float64Vec bool(boolean[] v) {
        try (var arena = Arena.ofConfined()) {
            byte[] vBytes = new byte[v.length];
            for (int i = 0; i < v.length; i++) vBytes[i] = v[i] ? (byte) 1 : (byte) 0;
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, vBytes);
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) v.length);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_BOOL.invokeExact(vSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Float64Vec i16(short[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_SHORT, v);
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) v.length);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_I16.invokeExact(vSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Float64Vec u16(short[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_SHORT, v);
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) v.length);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_U16.invokeExact(vSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Float64Vec isize(long[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_LONG, v);
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) v.length);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_ISIZE.invokeExact(vSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Float64Vec usize(long[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_LONG, v);
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) v.length);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_USIZE.invokeExact(vSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Float64Vec f64BeBytes(byte[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, v);
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) v.length);
            return new Float64Vec((MemorySegment) FLOAT64VEC_NEW_F64_BE_BYTES.invokeExact(vSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public double[] asSlice() {
        try (var arena = Arena.ofConfined()) {
            var resultSeg = (MemorySegment) FLOAT64VEC_AS_SLICE.invokeExact((SegmentAllocator) arena, handle);
            return ((MemorySegment) DiplomatLib.VH_SV_DATA.get(resultSeg, 0L))
                .reinterpret((long) DiplomatLib.VH_SV_LEN.get(resultSeg, 0L) * 8L)
                .toArray(ValueLayout.JAVA_DOUBLE);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void fillSlice(double[] v) {
        try (var arena = Arena.ofConfined()) {
            var vSeg = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, v);
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) v.length);
            FLOAT64VEC_FILL_SLICE.invokeExact(handle, vSlice);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setValue(double[] newSlice) {
        try (var arena = Arena.ofConfined()) {
            var newSliceSeg = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, newSlice);
            var newSliceSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(newSliceSlice, 0L, newSliceSeg);
            DiplomatLib.VH_SV_LEN.set(newSliceSlice, 0L, (long) newSlice.length);
            FLOAT64VEC_SET_VALUE.invokeExact(handle, newSliceSlice);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
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

    public double[] borrow() {
        try (var arena = Arena.ofConfined()) {
            var resultSeg = (MemorySegment) FLOAT64VEC_BORROW.invokeExact((SegmentAllocator) arena, handle);
            return ((MemorySegment) DiplomatLib.VH_SV_DATA.get(resultSeg, 0L))
                .reinterpret((long) DiplomatLib.VH_SV_LEN.get(resultSeg, 0L) * 8L)
                .toArray(ValueLayout.JAVA_DOUBLE);
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