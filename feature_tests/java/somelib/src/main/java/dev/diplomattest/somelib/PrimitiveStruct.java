package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class PrimitiveStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_FLOAT.withName("x"),
        ValueLayout.JAVA_BOOLEAN.withName("a"),
        MemoryLayout.paddingLayout(3),
        ValueLayout.JAVA_INT.withName("b"),
        MemoryLayout.paddingLayout(4),
        ValueLayout.JAVA_LONG.withName("c"),
        ValueLayout.JAVA_LONG.withName("d"),
        ValueLayout.JAVA_BYTE.withName("e"),
        MemoryLayout.paddingLayout(7)
    );
    private static final VarHandle VH_X = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("x"));
    private static final VarHandle VH_A = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"));
    private static final VarHandle VH_B = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("b"));
    private static final VarHandle VH_C = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("c"));
    private static final VarHandle VH_D = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("d"));
    private static final VarHandle VH_E = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("e"));
    private static final MethodHandle PRIMITIVESTRUCT_MUTABLE_SLICE;
    private static final MethodHandle PRIMITIVESTRUCT_MUTABLE_REF;

    static {
        PRIMITIVESTRUCT_MUTABLE_SLICE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("PrimitiveStruct_mutable_slice").orElseThrow(),
            FunctionDescriptor.ofVoid(DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        PRIMITIVESTRUCT_MUTABLE_REF = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("PrimitiveStruct_mutable_ref").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    public float x;

    public boolean a;

    public int b;

    public long c;

    public long d;

    public byte e;

    public PrimitiveStruct() {
    }

    PrimitiveStruct(float x, boolean a, int b, long c, long d, byte e) {
        this.x = x;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }

    static PrimitiveStruct fromNative(MemorySegment seg) {
        return new PrimitiveStruct(
            (float) VH_X.get(seg, 0L),
            (boolean) VH_A.get(seg, 0L),
            (int) VH_B.get(seg, 0L),
            (long) VH_C.get(seg, 0L),
            (long) VH_D.get(seg, 0L),
            (byte) VH_E.get(seg, 0L)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_X.set(seg, 0L, this.x);
        VH_A.set(seg, 0L, this.a);
        VH_B.set(seg, 0L, this.b);
        VH_C.set(seg, 0L, this.c);
        VH_D.set(seg, 0L, this.d);
        VH_E.set(seg, 0L, this.e);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.x = (float) VH_X.get(seg, 0L);
        this.a = (boolean) VH_A.get(seg, 0L);
        this.b = (int) VH_B.get(seg, 0L);
        this.c = (long) VH_C.get(seg, 0L);
        this.d = (long) VH_D.get(seg, 0L);
        this.e = (byte) VH_E.get(seg, 0L);
    }

    public static void mutableSlice(PrimitiveStruct[] a) {
        try (var arena = Arena.ofConfined()) {
            MemorySegment aSeg = arena.allocate(PrimitiveStruct.LAYOUT, a.length);
            for (int i = 0; i < a.length; i++) {
                aSeg.asSlice(i * PrimitiveStruct.LAYOUT.byteSize(), PrimitiveStruct.LAYOUT.byteSize())
                    .copyFrom(a[i].toNative(arena));
            }
            var aSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(aSlice, 0L, aSeg);
            DiplomatLib.VH_SV_LEN.set(aSlice, 0L, (long) a.length);
            PRIMITIVESTRUCT_MUTABLE_SLICE.invokeExact(aSlice);
            for (int i = 0; i < a.length; i++) {
                a[i].updateFromNative(
                    aSeg.asSlice(i * PrimitiveStruct.LAYOUT.byteSize(), PrimitiveStruct.LAYOUT.byteSize()));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void mutableRef(PrimitiveStruct a) {
        try (var arena = Arena.ofConfined()) {
            var selfSeg = this.toNative(arena);
            var aSeg = a.toNative(arena);
            PRIMITIVESTRUCT_MUTABLE_REF.invokeExact(selfSeg, aSeg);
            this.updateFromNative(selfSeg);
            a.updateFromNative(aSeg);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}