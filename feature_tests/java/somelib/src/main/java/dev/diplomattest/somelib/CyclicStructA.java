package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class CyclicStructA {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        CyclicStructB.LAYOUT.withName("a")
    );
    private static final long OFFSET_A = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("a"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle CYCLICSTRUCTA_GET_B;
    private static final MethodHandle CYCLICSTRUCTA_CYCLIC_OUT;
    private static final MethodHandle CYCLICSTRUCTA_NESTED_SLICE;
    private static final MethodHandle CYCLICSTRUCTA_DOUBLE_CYCLIC_OUT;
    private static final MethodHandle CYCLICSTRUCTA_GETTER_OUT;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        CYCLICSTRUCTA_GET_B = LINKER.downcallHandle(
            LIB.find("CyclicStructA_get_b").orElseThrow(),
            FunctionDescriptor.of(CyclicStructB.LAYOUT)
        );
        CYCLICSTRUCTA_CYCLIC_OUT = LINKER.downcallHandle(
            LIB.find("CyclicStructA_cyclic_out").orElseThrow(),
            FunctionDescriptor.ofVoid(CyclicStructA.LAYOUT, ValueLayout.ADDRESS)
        );
        CYCLICSTRUCTA_NESTED_SLICE = LINKER.downcallHandle(
            LIB.find("CyclicStructA_nested_slice").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_BYTE, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        CYCLICSTRUCTA_DOUBLE_CYCLIC_OUT = LINKER.downcallHandle(
            LIB.find("CyclicStructA_double_cyclic_out").orElseThrow(),
            FunctionDescriptor.ofVoid(CyclicStructA.LAYOUT, CyclicStructA.LAYOUT, ValueLayout.ADDRESS)
        );
        CYCLICSTRUCTA_GETTER_OUT = LINKER.downcallHandle(
            LIB.find("CyclicStructA_getter_out").orElseThrow(),
            FunctionDescriptor.ofVoid(CyclicStructA.LAYOUT, ValueLayout.ADDRESS)
        );
    }

    public CyclicStructB a;

    public CyclicStructA() {
    }

    CyclicStructA(CyclicStructB a) {
        this.a = a;
    }

    static CyclicStructA fromNative(MemorySegment seg) {
        return new CyclicStructA(
            CyclicStructB.fromNative(seg.asSlice(OFFSET_A, CyclicStructB.LAYOUT.byteSize()))
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.asSlice(OFFSET_A, CyclicStructB.LAYOUT.byteSize()).copyFrom(this.a.toNative(arena));
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.a = CyclicStructB.fromNative(seg.asSlice(OFFSET_A, CyclicStructB.LAYOUT.byteSize()));
    }

    public static CyclicStructB getB() {
        try (var arena = Arena.ofConfined()) {
            return CyclicStructB.fromNative((MemorySegment) CYCLICSTRUCTA_GET_B.invokeExact((SegmentAllocator) arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte nestedSlice(CyclicStructA[] sl) {
        try (var arena = Arena.ofConfined()) {
            MemorySegment slSeg = arena.allocate(CyclicStructA.LAYOUT, sl.length);
            for (int i = 0; i < sl.length; i++) {
                slSeg.asSlice(i * CyclicStructA.LAYOUT.byteSize(), CyclicStructA.LAYOUT.byteSize())
                    .copyFrom(sl[i].toNative(arena));
            }
            var slSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(slSlice, 0L, slSeg);
            DiplomatLib.VH_SV_LEN.set(slSlice, 0L, (long) sl.length);
            return (byte) CYCLICSTRUCTA_NESTED_SLICE.invokeExact(slSlice);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String cyclicOut() {
        var write = DiplomatLib.createWrite();
        try (var arena = Arena.ofConfined()) {
            CYCLICSTRUCTA_CYCLIC_OUT.invokeExact(this.toNative(arena), write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String doubleCyclicOut(CyclicStructA cyclicStructA) {
        var write = DiplomatLib.createWrite();
        try (var arena = Arena.ofConfined()) {
            CYCLICSTRUCTA_DOUBLE_CYCLIC_OUT.invokeExact(this.toNative(arena), cyclicStructA.toNative(arena), write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getterOut() {
        var write = DiplomatLib.createWrite();
        try (var arena = Arena.ofConfined()) {
            CYCLICSTRUCTA_GETTER_OUT.invokeExact(this.toNative(arena), write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}