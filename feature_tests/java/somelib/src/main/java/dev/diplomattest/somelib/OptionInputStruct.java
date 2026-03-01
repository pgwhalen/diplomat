package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class OptionInputStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.structLayout(ValueLayout.JAVA_BYTE, ValueLayout.JAVA_BOOLEAN).withName("a"),
        MemoryLayout.paddingLayout(2),
        MemoryLayout.structLayout(ValueLayout.JAVA_INT, ValueLayout.JAVA_BOOLEAN, MemoryLayout.paddingLayout(3)).withName("b"),
        MemoryLayout.structLayout(ValueLayout.JAVA_INT, ValueLayout.JAVA_BOOLEAN, MemoryLayout.paddingLayout(3)).withName("c")
    );

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
        var result = new OptionInputStruct();
        result.a = seg.get(ValueLayout.JAVA_BOOLEAN, 1L) ? (byte) seg.get(ValueLayout.JAVA_BYTE, 0L) : null;
        result.b = seg.get(ValueLayout.JAVA_BOOLEAN, 8L) ? (int) seg.get(ValueLayout.JAVA_INT, 4L) : null;
        result.c = seg.get(ValueLayout.JAVA_BOOLEAN, 16L) ? OptionEnum.fromNative((int) seg.get(ValueLayout.JAVA_INT, 12L)) : null;
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        if (this.a != null) { seg.set(ValueLayout.JAVA_BYTE, 0L, this.a); seg.set(ValueLayout.JAVA_BOOLEAN, 1L, true); } else { seg.set(ValueLayout.JAVA_BOOLEAN, 1L, false); }
        if (this.b != null) { seg.set(ValueLayout.JAVA_INT, 4L, this.b); seg.set(ValueLayout.JAVA_BOOLEAN, 8L, true); } else { seg.set(ValueLayout.JAVA_BOOLEAN, 8L, false); }
        if (this.c != null) { seg.set(ValueLayout.JAVA_INT, 12L, this.c.toNative()); seg.set(ValueLayout.JAVA_BOOLEAN, 16L, true); } else { seg.set(ValueLayout.JAVA_BOOLEAN, 16L, false); }
        return seg;
    }
}