package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class CallbackTestingStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("x"),
        ValueLayout.JAVA_INT.withName("y")
    );
    private static final VarHandle VH_X = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("x"));
    private static final VarHandle VH_Y = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("y"));

    public int x;

    public int y;

    public CallbackTestingStruct() {
    }

    CallbackTestingStruct(int x, int y) {
        this.x = x;
        this.y = y;
    }

    static CallbackTestingStruct fromNative(MemorySegment seg) {
        return new CallbackTestingStruct(
            (int) VH_X.get(seg, 0L),
            (int) VH_Y.get(seg, 0L)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_X.set(seg, 0L, this.x);
        VH_Y.set(seg, 0L, this.y);
        return seg;
    }
}