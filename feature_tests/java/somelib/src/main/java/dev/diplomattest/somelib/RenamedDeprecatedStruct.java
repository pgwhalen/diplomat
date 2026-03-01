package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class RenamedDeprecatedStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.paddingLayout(0)
    );

    public RenamedDeprecatedStruct() {
    }

    static RenamedDeprecatedStruct fromNative(MemorySegment seg) {
        var result = new RenamedDeprecatedStruct();
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        return seg;
    }
}