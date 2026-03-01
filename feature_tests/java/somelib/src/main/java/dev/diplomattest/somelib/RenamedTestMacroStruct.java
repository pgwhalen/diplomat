package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class RenamedTestMacroStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_LONG.withName("a")
    );

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle NAMESPACE_TESTMACROSTRUCT_TEST_FUNC;
    private static final MethodHandle NAMESPACE_TESTMACROSTRUCT_TEST_META;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        NAMESPACE_TESTMACROSTRUCT_TEST_FUNC = LINKER.downcallHandle(
            LIB.find("namespace_TestMacroStruct_test_func").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG)
        );
        NAMESPACE_TESTMACROSTRUCT_TEST_META = LINKER.downcallHandle(
            LIB.find("namespace_TestMacroStruct_test_meta").orElseThrow(),
            FunctionDescriptor.of(RenamedTestMacroStruct.LAYOUT)
        );
    }

    public long a;

    private RenamedTestMacroStruct(Void _internal) {
    }

    public RenamedTestMacroStruct() {
        try (var arena = Arena.ofConfined()) {
            var seg = (MemorySegment) NAMESPACE_TESTMACROSTRUCT_TEST_META.invokeExact((SegmentAllocator) arena);
            this.a = (long) seg.get(ValueLayout.JAVA_LONG, 0L);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static RenamedTestMacroStruct fromNative(MemorySegment seg) {
        var result = new RenamedTestMacroStruct((Void) null);
        result.a = (long) seg.get(ValueLayout.JAVA_LONG, 0L);
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.set(ValueLayout.JAVA_LONG, 0L, this.a);
        return seg;
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