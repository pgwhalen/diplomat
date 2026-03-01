package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class ImportedStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("foo"),
        ValueLayout.JAVA_BYTE.withName("count"),
        MemoryLayout.paddingLayout(3)
    );

    public int foo;

    public byte count;

    public ImportedStruct() {
    }

    ImportedStruct(int foo, byte count) {
        this.foo = foo;
        this.count = count;
    }

    static ImportedStruct fromNative(MemorySegment seg) {
        var result = new ImportedStruct();
        result.foo = (int) seg.get(ValueLayout.JAVA_INT, 0L);
        result.count = (byte) seg.get(ValueLayout.JAVA_BYTE, 4L);
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.set(ValueLayout.JAVA_INT, 0L, this.foo);
        seg.set(ValueLayout.JAVA_BYTE, 4L, this.count);
        return seg;
    }
}