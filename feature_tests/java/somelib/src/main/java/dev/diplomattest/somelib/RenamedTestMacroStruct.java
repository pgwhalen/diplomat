package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class RenamedTestMacroStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_LONG.withName("a")
    );
    private static final VarHandle VH_A = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"));
    private static final MethodHandle NAMESPACE_TESTMACROSTRUCT_TEST_FUNC;
    private static final MethodHandle NAMESPACE_TESTMACROSTRUCT_TEST_META;

    static {
        NAMESPACE_TESTMACROSTRUCT_TEST_FUNC = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_TestMacroStruct_test_func").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG)
        );
        NAMESPACE_TESTMACROSTRUCT_TEST_META = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_TestMacroStruct_test_meta").orElseThrow(),
            FunctionDescriptor.of(RenamedTestMacroStruct.LAYOUT)
        );
    }

    public long a;

    private RenamedTestMacroStruct(Void _internal) {
    }

    public RenamedTestMacroStruct() {
        try (var arena = Arena.ofConfined()) {
            var seg = (MemorySegment) NAMESPACE_TESTMACROSTRUCT_TEST_META.invokeExact((SegmentAllocator) arena);
            this.a = (long) VH_A.get(seg, 0L);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static RenamedTestMacroStruct fromNative(MemorySegment seg) {
        var result = new RenamedTestMacroStruct((Void) null);
        result.a = (long) VH_A.get(seg, 0L);
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_A.set(seg, 0L, this.a);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.a = (long) VH_A.get(seg, 0L);
    }

    public static long testFunc() {
        try {
            return (long) NAMESPACE_TESTMACROSTRUCT_TEST_FUNC.invokeExact();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}