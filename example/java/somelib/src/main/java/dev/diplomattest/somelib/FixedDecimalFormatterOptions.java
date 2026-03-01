package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class FixedDecimalFormatterOptions {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("groupingStrategy"),
        ValueLayout.JAVA_BOOLEAN.withName("someOtherConfig"),
        MemoryLayout.paddingLayout(3)
    );

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle ICU4X_FIXEDDECIMALFORMATTEROPTIONS_DEFAULT_MV1;

    static {
        System.loadLibrary("diplomat_example");
        LIB = SymbolLookup.loaderLookup();
        ICU4X_FIXEDDECIMALFORMATTEROPTIONS_DEFAULT_MV1 = LINKER.downcallHandle(
            LIB.find("icu4x_FixedDecimalFormatterOptions_default_mv1").orElseThrow(),
            FunctionDescriptor.of(FixedDecimalFormatterOptions.LAYOUT)
        );
    }

    public FixedDecimalGroupingStrategy groupingStrategy;

    public boolean someOtherConfig;

    private FixedDecimalFormatterOptions(Void _internal) {
    }

    public FixedDecimalFormatterOptions() {
        try (var arena = Arena.ofConfined()) {
            var seg = (MemorySegment) ICU4X_FIXEDDECIMALFORMATTEROPTIONS_DEFAULT_MV1.invokeExact((SegmentAllocator) arena);
            this.groupingStrategy = FixedDecimalGroupingStrategy.fromNative((int) seg.get(ValueLayout.JAVA_INT, 0L));
            this.someOtherConfig = (boolean) seg.get(ValueLayout.JAVA_BOOLEAN, 4L);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static FixedDecimalFormatterOptions fromNative(MemorySegment seg) {
        var result = new FixedDecimalFormatterOptions((Void) null);
        result.groupingStrategy = FixedDecimalGroupingStrategy.fromNative((int) seg.get(ValueLayout.JAVA_INT, 0L));
        result.someOtherConfig = (boolean) seg.get(ValueLayout.JAVA_BOOLEAN, 4L);
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.set(ValueLayout.JAVA_INT, 0L, this.groupingStrategy.toNative());
        seg.set(ValueLayout.JAVA_BOOLEAN, 4L, this.someOtherConfig);
        return seg;
    }
}