package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class BorrowingOptionStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.structLayout(DiplomatLib.DIPLOMAT_STRING_VIEW, ValueLayout.JAVA_BOOLEAN, MemoryLayout.paddingLayout(7)).withName("a")
    );

    public String a;

    public BorrowingOptionStruct() {
    }

    BorrowingOptionStruct(String a) {
        this.a = a;
    }

    static BorrowingOptionStruct fromNative(MemorySegment seg) {
        return new BorrowingOptionStruct(
            seg.get(ValueLayout.JAVA_BOOLEAN, 16L) ? new String(seg.get(ValueLayout.ADDRESS, 0L).reinterpret(seg.get(ValueLayout.JAVA_LONG, 8L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8) : null
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        if (this.a != null) { byte[] aBytes = this.a.getBytes(StandardCharsets.UTF_8); var aSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, aBytes); seg.set(ValueLayout.ADDRESS, 0L, aSeg); seg.set(ValueLayout.JAVA_LONG, 8L, (long) aBytes.length); seg.set(ValueLayout.JAVA_BOOLEAN, 16L, true); } else { seg.set(ValueLayout.JAVA_BOOLEAN, 16L, false); }
        return seg;
    }
}