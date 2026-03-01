package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class OptionInputStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("a"),
        ValueLayout.JAVA_BYTE.withName("b"),
        ValueLayout.JAVA_BYTE.withName("c")
    );

    public Object a;

    public Object b;

    public Object c;

    public OptionInputStruct() {
    }

    OptionInputStruct(Object a, Object b, Object c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    static OptionInputStruct fromNative(MemorySegment seg) {
        var result = new OptionInputStruct();
        result.a = null /* unsupported field a */;
        result.b = null /* unsupported field b */;
        result.c = null /* unsupported field c */;
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        // unsupported field a
        // unsupported field b
        // unsupported field c
        return seg;
    }
}