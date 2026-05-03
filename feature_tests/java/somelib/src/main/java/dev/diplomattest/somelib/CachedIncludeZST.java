package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class CachedIncludeZST {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.paddingLayout(0)
    );

    public CachedIncludeZST() {
    }

    static CachedIncludeZST fromNative(MemorySegment seg) {
        return new CachedIncludeZST();
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
    }
}