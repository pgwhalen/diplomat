package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class BorrowedFieldsReturning {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("bytes")
    );

    public String bytes;

    public BorrowedFieldsReturning() {
    }

    BorrowedFieldsReturning(String bytes) {
        this.bytes = bytes;
    }

    static BorrowedFieldsReturning fromNative(MemorySegment seg) {
        return new BorrowedFieldsReturning(
            new String(seg.get(ValueLayout.ADDRESS, 0L).reinterpret(seg.get(ValueLayout.JAVA_LONG, 8L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        { byte[] bytesBytes = this.bytes.getBytes(StandardCharsets.UTF_8); var bytesSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, bytesBytes); seg.set(ValueLayout.ADDRESS, 0L, bytesSeg); seg.set(ValueLayout.JAVA_LONG, 8L, (long) bytesBytes.length); }
        return seg;
    }
}