package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class OptionStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("a"),
        ValueLayout.ADDRESS.withName("b"),
        ValueLayout.JAVA_INT.withName("c"),
        MemoryLayout.paddingLayout(4),
        ValueLayout.ADDRESS.withName("d")
    );

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
        var result = new OptionStruct();
        result.a = new OptionOpaque(seg.get(ValueLayout.ADDRESS, 0L));
        result.b = new OptionOpaqueChar(seg.get(ValueLayout.ADDRESS, 8L));
        result.c = (int) seg.get(ValueLayout.JAVA_INT, 16L);
        result.d = new OptionOpaque(seg.get(ValueLayout.ADDRESS, 24L));
        return result;
    }
}