package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class ErrorStruct extends RuntimeException {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("i"),
        ValueLayout.JAVA_INT.withName("j")
    );
    private static final VarHandle VH_I = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("i"));
    private static final VarHandle VH_J = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("j"));

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
            (int) VH_I.get(seg, 0L),
            (int) VH_J.get(seg, 0L)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_I.set(seg, 0L, this.i);
        VH_J.set(seg, 0L, this.j);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.i = (int) VH_I.get(seg, 0L);
        this.j = (int) VH_J.get(seg, 0L);
    }
}