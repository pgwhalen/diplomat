package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class CyclicStructB {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("field")
    );

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle CYCLICSTRUCTB_GET_A;
    private static final MethodHandle CYCLICSTRUCTB_GET_A_OPTION;
    static final StructLayout CYCLICSTRUCTB_GET_A_OPTION_RESULT = MemoryLayout.structLayout(
            CyclicStructA.LAYOUT.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok")
        );

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        CYCLICSTRUCTB_GET_A = LINKER.downcallHandle(
            LIB.find("CyclicStructB_get_a").orElseThrow(),
            FunctionDescriptor.of(CyclicStructA.LAYOUT)
        );
        CYCLICSTRUCTB_GET_A_OPTION = LINKER.downcallHandle(
            LIB.find("CyclicStructB_get_a_option").orElseThrow(),
            FunctionDescriptor.of(CYCLICSTRUCTB_GET_A_OPTION_RESULT)
        );
    }

    public byte field;

    public CyclicStructB() {
    }

    CyclicStructB(byte field) {
        this.field = field;
    }

    static CyclicStructB fromNative(MemorySegment seg) {
        var result = new CyclicStructB();
        result.field = (byte) seg.get(ValueLayout.JAVA_BYTE, 0L);
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.set(ValueLayout.JAVA_BYTE, 0L, this.field);
        return seg;
    }

    public static CyclicStructA getA() {
        try (var arena = Arena.ofConfined()) {
            return CyclicStructA.fromNative((MemorySegment) CYCLICSTRUCTB_GET_A.invokeExact((SegmentAllocator) arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Optional<CyclicStructA> getAOption() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) CYCLICSTRUCTB_GET_A_OPTION.invokeExact((SegmentAllocator) arena);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 1L);
            if (isOk) {
                return Optional.of(CyclicStructA.fromNative(result.asSlice(0L, CyclicStructA.LAYOUT.byteSize())));
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