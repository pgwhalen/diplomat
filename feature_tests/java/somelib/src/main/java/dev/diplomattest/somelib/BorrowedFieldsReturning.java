package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class BorrowedFieldsReturning {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("bytes")
    );
    private static final VarHandle VH_BYTES_DATA = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("bytes"), MemoryLayout.PathElement.groupElement("data"));
    private static final VarHandle VH_BYTES_LEN = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("bytes"), MemoryLayout.PathElement.groupElement("len"));

    public String bytes;

    public BorrowedFieldsReturning() {
    }

    BorrowedFieldsReturning(String bytes) {
        this.bytes = bytes;
    }

    static BorrowedFieldsReturning fromNative(MemorySegment seg) {
        return new BorrowedFieldsReturning(
            new String(((MemorySegment) VH_BYTES_DATA.get(seg, 0L)).reinterpret((long) VH_BYTES_LEN.get(seg, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        { byte[] bytesBytes = this.bytes.getBytes(StandardCharsets.UTF_8); var bytesSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, bytesBytes); VH_BYTES_DATA.set(seg, 0L, bytesSeg); VH_BYTES_LEN.set(seg, 0L, (long) bytesBytes.length); }
        return seg;
    }
}