package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PrimitiveStructVec implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle PRIMITIVESTRUCTVEC_NEW;
    private static final MethodHandle PRIMITIVESTRUCTVEC_PUSH;
    private static final MethodHandle PRIMITIVESTRUCTVEC_LEN;
    private static final MethodHandle PRIMITIVESTRUCTVEC_AS_SLICE;
    private static final MethodHandle PRIMITIVESTRUCTVEC_AS_SLICE_MUT;
    private static final MethodHandle PRIMITIVESTRUCTVEC_GET;
    private static final MethodHandle PRIMITIVESTRUCTVEC_TAKE_SLICE_FROM_OTHER_NAMESPACE;
    private static final MethodHandle PRIMITIVESTRUCTVEC_TAKE_IN_SLICE;
    static final StructLayout PRIMITIVESTRUCTVEC_GET_RESULT = MemoryLayout.structLayout(
            PrimitiveStruct.LAYOUT.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("PrimitiveStructVec_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        PRIMITIVESTRUCTVEC_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("PrimitiveStructVec_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
        PRIMITIVESTRUCTVEC_PUSH = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("PrimitiveStructVec_push").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, PrimitiveStruct.LAYOUT)
        );
        PRIMITIVESTRUCTVEC_LEN = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("PrimitiveStructVec_len").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
        );
        PRIMITIVESTRUCTVEC_AS_SLICE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("PrimitiveStructVec_as_slice").orElseThrow(),
            FunctionDescriptor.of(DiplomatLib.DIPLOMAT_STRING_VIEW, ValueLayout.ADDRESS)
        );
        PRIMITIVESTRUCTVEC_AS_SLICE_MUT = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("PrimitiveStructVec_as_slice_mut").orElseThrow(),
            FunctionDescriptor.of(DiplomatLib.DIPLOMAT_STRING_VIEW, ValueLayout.ADDRESS)
        );
        PRIMITIVESTRUCTVEC_GET = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("PrimitiveStructVec_get").orElseThrow(),
            FunctionDescriptor.of(PRIMITIVESTRUCTVEC_GET_RESULT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        PRIMITIVESTRUCTVEC_TAKE_SLICE_FROM_OTHER_NAMESPACE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("PrimitiveStructVec_take_slice_from_other_namespace").orElseThrow(),
            FunctionDescriptor.ofVoid(DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        PRIMITIVESTRUCTVEC_TAKE_IN_SLICE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("PrimitiveStructVec_take_in_slice").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
    }

    final MemorySegment handle;

    PrimitiveStructVec(MemorySegment handle) {
        this.handle = handle;
    }

    public PrimitiveStructVec() {
        try {
            this.handle = (MemorySegment) PRIMITIVESTRUCTVEC_NEW.invokeExact();
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

    public static void takeSliceFromOtherNamespace(RenamedStructWithAttrs[] sl) {
        try (var arena = Arena.ofConfined()) {
            MemorySegment slSeg = arena.allocate(RenamedStructWithAttrs.LAYOUT, sl.length);
            for (int i = 0; i < sl.length; i++) {
                slSeg.asSlice(i * RenamedStructWithAttrs.LAYOUT.byteSize(), RenamedStructWithAttrs.LAYOUT.byteSize())
                    .copyFrom(sl[i].toNative(arena));
            }
            var slSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(slSlice, 0L, slSeg);
            DiplomatLib.VH_SV_LEN.set(slSlice, 0L, (long) sl.length);
            PRIMITIVESTRUCTVEC_TAKE_SLICE_FROM_OTHER_NAMESPACE.invokeExact(slSlice);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static PrimitiveStructVec takeInSlice(PrimitiveStruct[] a) {
        try (var arena = Arena.ofConfined()) {
            MemorySegment aSeg = arena.allocate(PrimitiveStruct.LAYOUT, a.length);
            for (int i = 0; i < a.length; i++) {
                aSeg.asSlice(i * PrimitiveStruct.LAYOUT.byteSize(), PrimitiveStruct.LAYOUT.byteSize())
                    .copyFrom(a[i].toNative(arena));
            }
            var aSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(aSlice, 0L, aSeg);
            DiplomatLib.VH_SV_LEN.set(aSlice, 0L, (long) a.length);
            return new PrimitiveStructVec((MemorySegment) PRIMITIVESTRUCTVEC_TAKE_IN_SLICE.invokeExact(aSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void push(PrimitiveStruct value) {
        try (var arena = Arena.ofConfined()) {
            PRIMITIVESTRUCTVEC_PUSH.invokeExact(handle, value.toNative(arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public long len() {
        try {
            return (long) PRIMITIVESTRUCTVEC_LEN.invokeExact(handle);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public PrimitiveStruct[] asSlice() {
        try (var arena = Arena.ofConfined()) {
            var resultSeg = (MemorySegment) PRIMITIVESTRUCTVEC_AS_SLICE.invokeExact((SegmentAllocator) arena, handle);
            MemorySegment dataPtr = (MemorySegment) DiplomatLib.VH_SV_DATA.get(resultSeg, 0L);
            long count = (long) DiplomatLib.VH_SV_LEN.get(resultSeg, 0L);
            var data = dataPtr.reinterpret(count * PrimitiveStruct.LAYOUT.byteSize());
            PrimitiveStruct[] arr = new PrimitiveStruct[(int) count];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = PrimitiveStruct.fromNative(data.asSlice(i * PrimitiveStruct.LAYOUT.byteSize(), PrimitiveStruct.LAYOUT.byteSize()));
            }
            return arr;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public PrimitiveStruct[] asSliceMut() {
        try (var arena = Arena.ofConfined()) {
            var resultSeg = (MemorySegment) PRIMITIVESTRUCTVEC_AS_SLICE_MUT.invokeExact((SegmentAllocator) arena, handle);
            MemorySegment dataPtr = (MemorySegment) DiplomatLib.VH_SV_DATA.get(resultSeg, 0L);
            long count = (long) DiplomatLib.VH_SV_LEN.get(resultSeg, 0L);
            var data = dataPtr.reinterpret(count * PrimitiveStruct.LAYOUT.byteSize());
            PrimitiveStruct[] arr = new PrimitiveStruct[(int) count];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = PrimitiveStruct.fromNative(data.asSlice(i * PrimitiveStruct.LAYOUT.byteSize(), PrimitiveStruct.LAYOUT.byteSize()));
            }
            return arr;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Optional<PrimitiveStruct> get(long idx) {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) PRIMITIVESTRUCTVEC_GET.invokeExact((SegmentAllocator) arena, handle, idx);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 40L);
            if (isOk) {
                return Optional.of(PrimitiveStruct.fromNative(result.asSlice(0L, PrimitiveStruct.LAYOUT.byteSize())));
            } else {
                return Optional.empty();
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}