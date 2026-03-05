package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

/**
 * Testing JS-specific layout/padding behavior
 */
public class ScalarPairWithPadding {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("first"),
        MemoryLayout.paddingLayout(3),
        ValueLayout.JAVA_INT.withName("second")
    );
    private static final VarHandle VH_FIRST = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("first"));
    private static final VarHandle VH_SECOND = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("second"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle SCALARPAIRWITHPADDING_ASSERT_VALUE;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        SCALARPAIRWITHPADDING_ASSERT_VALUE = LINKER.downcallHandle(
            LIB.find("ScalarPairWithPadding_assert_value").orElseThrow(),
            FunctionDescriptor.ofVoid(ScalarPairWithPadding.LAYOUT)
        );
    }

    public byte first;

    public int second;

    public ScalarPairWithPadding() {
    }

    ScalarPairWithPadding(byte first, int second) {
        this.first = first;
        this.second = second;
    }

    static ScalarPairWithPadding fromNative(MemorySegment seg) {
        return new ScalarPairWithPadding(
            (byte) VH_FIRST.get(seg, 0L),
            (int) VH_SECOND.get(seg, 0L)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_FIRST.set(seg, 0L, this.first);
        VH_SECOND.set(seg, 0L, this.second);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.first = (byte) VH_FIRST.get(seg, 0L);
        this.second = (int) VH_SECOND.get(seg, 0L);
    }

    public void assertValue() {
        try (var arena = Arena.ofConfined()) {
            SCALARPAIRWITHPADDING_ASSERT_VALUE.invokeExact(this.toNative(arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}