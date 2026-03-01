package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class OptionInputStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.structLayout(ValueLayout.JAVA_BYTE.withName("value"), ValueLayout.JAVA_BOOLEAN.withName("is_ok")).withName("a"),
        MemoryLayout.paddingLayout(2),
        MemoryLayout.structLayout(ValueLayout.JAVA_INT.withName("value"), ValueLayout.JAVA_BOOLEAN.withName("is_ok"), MemoryLayout.paddingLayout(3)).withName("b"),
        MemoryLayout.structLayout(ValueLayout.JAVA_INT.withName("value"), ValueLayout.JAVA_BOOLEAN.withName("is_ok"), MemoryLayout.paddingLayout(3)).withName("c")
    );
    private static final VarHandle VH_A_VALUE = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"), MemoryLayout.PathElement.groupElement("value"));
    private static final VarHandle VH_A_IS_OK = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"), MemoryLayout.PathElement.groupElement("is_ok"));
    private static final VarHandle VH_B_VALUE = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("b"), MemoryLayout.PathElement.groupElement("value"));
    private static final VarHandle VH_B_IS_OK = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("b"), MemoryLayout.PathElement.groupElement("is_ok"));
    private static final VarHandle VH_C_VALUE = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("c"), MemoryLayout.PathElement.groupElement("value"));
    private static final VarHandle VH_C_IS_OK = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("c"), MemoryLayout.PathElement.groupElement("is_ok"));

    public Byte a;

    public Integer b;

    public OptionEnum c;

    public OptionInputStruct() {
    }

    OptionInputStruct(Byte a, Integer b, OptionEnum c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    static OptionInputStruct fromNative(MemorySegment seg) {
        return new OptionInputStruct(
            (boolean) VH_A_IS_OK.get(seg, 0L) ? (byte) VH_A_VALUE.get(seg, 0L) : null,
            (boolean) VH_B_IS_OK.get(seg, 0L) ? (int) VH_B_VALUE.get(seg, 0L) : null,
            (boolean) VH_C_IS_OK.get(seg, 0L) ? OptionEnum.fromNative((int) VH_C_VALUE.get(seg, 0L)) : null
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        if (this.a != null) { VH_A_VALUE.set(seg, 0L, this.a); VH_A_IS_OK.set(seg, 0L, true); } else { VH_A_IS_OK.set(seg, 0L, false); }
        if (this.b != null) { VH_B_VALUE.set(seg, 0L, this.b); VH_B_IS_OK.set(seg, 0L, true); } else { VH_B_IS_OK.set(seg, 0L, false); }
        if (this.c != null) { VH_C_VALUE.set(seg, 0L, this.c.toNative()); VH_C_IS_OK.set(seg, 0L, true); } else { VH_C_IS_OK.set(seg, 0L, false); }
        return seg;
    }
}