package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class OptionStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("a"),
        ValueLayout.ADDRESS.withName("b"),
        ValueLayout.JAVA_INT.withName("c"),
        MemoryLayout.paddingLayout(4),
        ValueLayout.ADDRESS.withName("d")
    );
    private static final VarHandle VH_A = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"));
    private static final VarHandle VH_B = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("b"));
    private static final VarHandle VH_C = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("c"));
    private static final VarHandle VH_D = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("d"));

    public OptionOpaque a;

    public OptionOpaqueChar b;

    public int c;

    public OptionOpaque d;

    public OptionStruct() {
    }

    OptionStruct(OptionOpaque a, OptionOpaqueChar b, int c, OptionOpaque d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    static OptionStruct fromNative(MemorySegment seg) {
        return new OptionStruct(
            new OptionOpaque((MemorySegment) VH_A.get(seg, 0L)),
            new OptionOpaqueChar((MemorySegment) VH_B.get(seg, 0L)),
            (int) VH_C.get(seg, 0L),
            new OptionOpaque((MemorySegment) VH_D.get(seg, 0L))
        );
    }
}