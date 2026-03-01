package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class BorrowedFields {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("a"),
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("b"),
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("c")
    );
    private static final VarHandle VH_A_DATA = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"), MemoryLayout.PathElement.groupElement("data"));
    private static final VarHandle VH_A_LEN = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"), MemoryLayout.PathElement.groupElement("len"));
    private static final VarHandle VH_B_DATA = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("b"), MemoryLayout.PathElement.groupElement("data"));
    private static final VarHandle VH_B_LEN = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("b"), MemoryLayout.PathElement.groupElement("len"));
    private static final VarHandle VH_C_DATA = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("c"), MemoryLayout.PathElement.groupElement("data"));
    private static final VarHandle VH_C_LEN = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("c"), MemoryLayout.PathElement.groupElement("len"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle BORROWEDFIELDS_FROM_BAR_AND_STRINGS;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        BORROWEDFIELDS_FROM_BAR_AND_STRINGS = LINKER.downcallHandle(
            LIB.find("BorrowedFields_from_bar_and_strings").orElseThrow(),
            FunctionDescriptor.of(BorrowedFields.LAYOUT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    public String a;

    public String b;

    public String c;

    public BorrowedFields() {
    }

    BorrowedFields(String a, String b, String c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    static BorrowedFields fromNative(MemorySegment seg) {
        return new BorrowedFields(
            new String(((MemorySegment) VH_A_DATA.get(seg, 0L)).reinterpret((long) VH_A_LEN.get(seg, 0L) * 2).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_16LE),
            new String(((MemorySegment) VH_B_DATA.get(seg, 0L)).reinterpret((long) VH_B_LEN.get(seg, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8),
            new String(((MemorySegment) VH_C_DATA.get(seg, 0L)).reinterpret((long) VH_C_LEN.get(seg, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        { byte[] aBytes = this.a.getBytes(StandardCharsets.UTF_16LE); var aSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, aBytes); VH_A_DATA.set(seg, 0L, aSeg); VH_A_LEN.set(seg, 0L, (long) aBytes.length / 2); }
        { byte[] bBytes = this.b.getBytes(StandardCharsets.UTF_8); var bSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, bBytes); VH_B_DATA.set(seg, 0L, bSeg); VH_B_LEN.set(seg, 0L, (long) bBytes.length); }
        { byte[] cBytes = this.c.getBytes(StandardCharsets.UTF_8); var cSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, cBytes); VH_C_DATA.set(seg, 0L, cSeg); VH_C_LEN.set(seg, 0L, (long) cBytes.length); }
        return seg;
    }

    public static BorrowedFields fromBarAndStrings(Bar bar, String dstr16, String utf8Str) {
        try (var arena = Arena.ofConfined()) {
            char[] dstr16Chars = dstr16.toCharArray();

            var dstr16Seg = arena.allocateFrom(ValueLayout.JAVA_CHAR, dstr16Chars);
            byte[] utf8StrBytes = utf8Str.getBytes(StandardCharsets.UTF_8);

            var utf8StrSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, utf8StrBytes);
            return BorrowedFields.fromNative((MemorySegment) BORROWEDFIELDS_FROM_BAR_AND_STRINGS.invokeExact((SegmentAllocator) arena, bar.handle, dstr16Seg, (long) dstr16Chars.length, utf8StrSeg, (long) utf8StrBytes.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}