package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class FixedDecimalFormatterOptions {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("groupingStrategy"),
        ValueLayout.JAVA_BOOLEAN.withName("someOtherConfig"),
        MemoryLayout.paddingLayout(3)
    );
    private static final VarHandle VH_GROUPING_STRATEGY = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("groupingStrategy"));
    private static final VarHandle VH_SOME_OTHER_CONFIG = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("someOtherConfig"));
    private static final MethodHandle ICU4X_FIXEDDECIMALFORMATTEROPTIONS_DEFAULT_MV1;

    static {
        ICU4X_FIXEDDECIMALFORMATTEROPTIONS_DEFAULT_MV1 = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("icu4x_FixedDecimalFormatterOptions_default_mv1").orElseThrow(),
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
            this.groupingStrategy = FixedDecimalGroupingStrategy.fromNative((int) VH_GROUPING_STRATEGY.get(seg, 0L));
            this.someOtherConfig = (boolean) VH_SOME_OTHER_CONFIG.get(seg, 0L);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static FixedDecimalFormatterOptions fromNative(MemorySegment seg) {
        var result = new FixedDecimalFormatterOptions((Void) null);
        result.groupingStrategy = FixedDecimalGroupingStrategy.fromNative((int) VH_GROUPING_STRATEGY.get(seg, 0L));
        result.someOtherConfig = (boolean) VH_SOME_OTHER_CONFIG.get(seg, 0L);
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_GROUPING_STRATEGY.set(seg, 0L, this.groupingStrategy.toNative());
        VH_SOME_OTHER_CONFIG.set(seg, 0L, this.someOtherConfig);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.groupingStrategy = FixedDecimalGroupingStrategy.fromNative((int) VH_GROUPING_STRATEGY.get(seg, 0L));
        this.someOtherConfig = (boolean) VH_SOME_OTHER_CONFIG.get(seg, 0L);
    }
}