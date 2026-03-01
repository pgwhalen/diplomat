package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class MyZst extends RuntimeException {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.paddingLayout(0)
    );

    public MyZst() {
        super("MyZst");
    }

    static MyZst fromNative(MemorySegment seg) {
        return new MyZst();
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        return seg;
    }
}