package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class CyclicStructC {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        CyclicStructA.LAYOUT.withName("a")
    );
    private static final long OFFSET_A = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("a"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle CYCLICSTRUCTC_TAKES_NESTED_PARAMETERS;
    private static final MethodHandle CYCLICSTRUCTC_CYCLIC_OUT;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        CYCLICSTRUCTC_TAKES_NESTED_PARAMETERS = LINKER.downcallHandle(
            LIB.find("CyclicStructC_takes_nested_parameters").orElseThrow(),
            FunctionDescriptor.of(CyclicStructC.LAYOUT, CyclicStructC.LAYOUT)
        );
        CYCLICSTRUCTC_CYCLIC_OUT = LINKER.downcallHandle(
            LIB.find("CyclicStructC_cyclic_out").orElseThrow(),
            FunctionDescriptor.ofVoid(CyclicStructC.LAYOUT, ValueLayout.ADDRESS)
        );
    }

    public CyclicStructA a;

    public CyclicStructC() {
    }

    CyclicStructC(CyclicStructA a) {
        this.a = a;
    }

    static CyclicStructC fromNative(MemorySegment seg) {
        return new CyclicStructC(
            CyclicStructA.fromNative(seg.asSlice(OFFSET_A, CyclicStructA.LAYOUT.byteSize()))
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.asSlice(OFFSET_A, CyclicStructA.LAYOUT.byteSize()).copyFrom(this.a.toNative(arena));
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.a = CyclicStructA.fromNative(seg.asSlice(OFFSET_A, CyclicStructA.LAYOUT.byteSize()));
    }

    public static CyclicStructC takesNestedParameters(CyclicStructC c) {
        try (var arena = Arena.ofConfined()) {
            return CyclicStructC.fromNative((MemorySegment) CYCLICSTRUCTC_TAKES_NESTED_PARAMETERS.invokeExact((SegmentAllocator) arena, c.toNative(arena)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String cyclicOut() {
        var write = DiplomatLib.createWrite();
        try (var arena = Arena.ofConfined()) {
            CYCLICSTRUCTC_CYCLIC_OUT.invokeExact(this.toNative(arena), write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}