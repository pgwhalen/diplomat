package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class BorrowedFieldsReturning {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("bytes")
    );

    public Object bytes;

    public BorrowedFieldsReturning() {
    }

    BorrowedFieldsReturning(Object bytes) {
        this.bytes = bytes;
    }

    static BorrowedFieldsReturning fromNative(MemorySegment seg) {
        var result = new BorrowedFieldsReturning();
        result.bytes = null /* unsupported field bytes */;
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        // unsupported field bytes
        return seg;
    }
}