package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class ErrorStruct extends RuntimeException {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("i"),
        ValueLayout.JAVA_INT.withName("j")
    );

    public int i;

    public int j;

    public ErrorStruct() {
        super("ErrorStruct");
    }

    ErrorStruct(int i, int j) {
        super("ErrorStruct");
        this.i = i;
        this.j = j;
    }

    static ErrorStruct fromNative(MemorySegment seg) {
        return new ErrorStruct(
            (int) seg.get(ValueLayout.JAVA_INT, 0L),
            (int) seg.get(ValueLayout.JAVA_INT, 4L)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.set(ValueLayout.JAVA_INT, 0L, this.i);
        seg.set(ValueLayout.JAVA_INT, 4L, this.j);
        return seg;
    }
}