package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class BorrowingOptionStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.structLayout(DiplomatLib.DIPLOMAT_STRING_VIEW.withName("value"), ValueLayout.JAVA_BOOLEAN.withName("is_ok"), MemoryLayout.paddingLayout(7)).withName("a")
    );
    private static final VarHandle VH_A_DATA = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"), MemoryLayout.PathElement.groupElement("value"), MemoryLayout.PathElement.groupElement("data"));
    private static final VarHandle VH_A_LEN = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"), MemoryLayout.PathElement.groupElement("value"), MemoryLayout.PathElement.groupElement("len"));
    private static final VarHandle VH_A_IS_OK = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"), MemoryLayout.PathElement.groupElement("is_ok"));

    public String a;

    public BorrowingOptionStruct() {
    }

    BorrowingOptionStruct(String a) {
        this.a = a;
    }

    static BorrowingOptionStruct fromNative(MemorySegment seg) {
        return new BorrowingOptionStruct(
            (boolean) VH_A_IS_OK.get(seg, 0L) ? new String(((MemorySegment) VH_A_DATA.get(seg, 0L)).reinterpret((long) VH_A_LEN.get(seg, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8) : null
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        if (this.a != null) { byte[] aBytes = this.a.getBytes(StandardCharsets.UTF_8); var aSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, aBytes); VH_A_DATA.set(seg, 0L, aSeg); VH_A_LEN.set(seg, 0L, (long) aBytes.length); VH_A_IS_OK.set(seg, 0L, true); } else { VH_A_IS_OK.set(seg, 0L, false); }
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.a = (boolean) VH_A_IS_OK.get(seg, 0L) ? new String(((MemorySegment) VH_A_DATA.get(seg, 0L)).reinterpret((long) VH_A_LEN.get(seg, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8) : null;
    }
}