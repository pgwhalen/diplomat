package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class CyclicStructA {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        CyclicStructB.LAYOUT.withName("a")
    );

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle CYCLICSTRUCTA_GET_B;
    private static final MethodHandle CYCLICSTRUCTA_CYCLIC_OUT;
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
        var result = new CyclicStructA();
        result.a = CyclicStructB.fromNative(seg.asSlice(0L, CyclicStructB.LAYOUT.byteSize()));
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.asSlice(0L, CyclicStructB.LAYOUT.byteSize()).copyFrom(this.a.toNative(arena));
        return seg;
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