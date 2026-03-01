package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class BorrowingOptionStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("a")
    );

    public Object a;

    public BorrowingOptionStruct() {
    }

    BorrowingOptionStruct(Object a) {
        this.a = a;
    }

    static BorrowingOptionStruct fromNative(MemorySegment seg) {
        var result = new BorrowingOptionStruct();
        result.a = null /* unsupported field a */;
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        // unsupported field a
        return seg;
    }
}