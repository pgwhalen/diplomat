package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class PrimitiveStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_FLOAT.withName("x"),
        ValueLayout.JAVA_BOOLEAN.withName("a"),
        MemoryLayout.paddingLayout(3),
        ValueLayout.JAVA_INT.withName("b"),
        MemoryLayout.paddingLayout(4),
        ValueLayout.JAVA_LONG.withName("c"),
        ValueLayout.JAVA_LONG.withName("d"),
        ValueLayout.JAVA_BYTE.withName("e"),
        MemoryLayout.paddingLayout(7)
    );

    public float x;

    public boolean a;

    public int b;

    public long c;

    public long d;

    public byte e;

    public PrimitiveStruct() {
    }

    PrimitiveStruct(float x, boolean a, int b, long c, long d, byte e) {
        this.x = x;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }

    static PrimitiveStruct fromNative(MemorySegment seg) {
        var result = new PrimitiveStruct();
        result.x = (float) seg.get(ValueLayout.JAVA_FLOAT, 0L);
        result.a = (boolean) seg.get(ValueLayout.JAVA_BOOLEAN, 4L);
        result.b = (int) seg.get(ValueLayout.JAVA_INT, 8L);
        result.c = (long) seg.get(ValueLayout.JAVA_LONG, 16L);
        result.d = (long) seg.get(ValueLayout.JAVA_LONG, 24L);
        result.e = (byte) seg.get(ValueLayout.JAVA_BYTE, 32L);
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.set(ValueLayout.JAVA_FLOAT, 0L, this.x);
        seg.set(ValueLayout.JAVA_BOOLEAN, 4L, this.a);
        seg.set(ValueLayout.JAVA_INT, 8L, this.b);
        seg.set(ValueLayout.JAVA_LONG, 16L, this.c);
        seg.set(ValueLayout.JAVA_LONG, 24L, this.d);
        seg.set(ValueLayout.JAVA_BYTE, 32L, this.e);
        return seg;
    }
}