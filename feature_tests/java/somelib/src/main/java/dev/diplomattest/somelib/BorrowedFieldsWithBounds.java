package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class BorrowedFieldsWithBounds {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("fieldA"),
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("fieldB"),
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("fieldC")
    );
    private static final VarHandle VH_FIELD_A_DATA = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fieldA"), MemoryLayout.PathElement.groupElement("data"));
    private static final VarHandle VH_FIELD_A_LEN = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fieldA"), MemoryLayout.PathElement.groupElement("len"));
    private static final VarHandle VH_FIELD_B_DATA = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fieldB"), MemoryLayout.PathElement.groupElement("data"));
    private static final VarHandle VH_FIELD_B_LEN = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fieldB"), MemoryLayout.PathElement.groupElement("len"));
    private static final VarHandle VH_FIELD_C_DATA = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fieldC"), MemoryLayout.PathElement.groupElement("data"));
    private static final VarHandle VH_FIELD_C_LEN = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fieldC"), MemoryLayout.PathElement.groupElement("len"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle BORROWEDFIELDSWITHBOUNDS_FROM_FOO_AND_STRINGS;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        BORROWEDFIELDSWITHBOUNDS_FROM_FOO_AND_STRINGS = LINKER.downcallHandle(
            LIB.find("BorrowedFieldsWithBounds_from_foo_and_strings").orElseThrow(),
            FunctionDescriptor.of(BorrowedFieldsWithBounds.LAYOUT, ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
    }

    public String fieldA;

    public String fieldB;

    public String fieldC;

    public BorrowedFieldsWithBounds() {
    }

    BorrowedFieldsWithBounds(String fieldA, String fieldB, String fieldC) {
        this.fieldA = fieldA;
        this.fieldB = fieldB;
        this.fieldC = fieldC;
    }

    static BorrowedFieldsWithBounds fromNative(MemorySegment seg) {
        return new BorrowedFieldsWithBounds(
            new String(((MemorySegment) VH_FIELD_A_DATA.get(seg, 0L)).reinterpret((long) VH_FIELD_A_LEN.get(seg, 0L) * 2).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_16LE),
            new String(((MemorySegment) VH_FIELD_B_DATA.get(seg, 0L)).reinterpret((long) VH_FIELD_B_LEN.get(seg, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8),
            new String(((MemorySegment) VH_FIELD_C_DATA.get(seg, 0L)).reinterpret((long) VH_FIELD_C_LEN.get(seg, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        { byte[] fieldABytes = this.fieldA.getBytes(StandardCharsets.UTF_16LE); var fieldASeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, fieldABytes); VH_FIELD_A_DATA.set(seg, 0L, fieldASeg); VH_FIELD_A_LEN.set(seg, 0L, (long) fieldABytes.length / 2); }
        { byte[] fieldBBytes = this.fieldB.getBytes(StandardCharsets.UTF_8); var fieldBSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, fieldBBytes); VH_FIELD_B_DATA.set(seg, 0L, fieldBSeg); VH_FIELD_B_LEN.set(seg, 0L, (long) fieldBBytes.length); }
        { byte[] fieldCBytes = this.fieldC.getBytes(StandardCharsets.UTF_8); var fieldCSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, fieldCBytes); VH_FIELD_C_DATA.set(seg, 0L, fieldCSeg); VH_FIELD_C_LEN.set(seg, 0L, (long) fieldCBytes.length); }
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.fieldA = new String(((MemorySegment) VH_FIELD_A_DATA.get(seg, 0L)).reinterpret((long) VH_FIELD_A_LEN.get(seg, 0L) * 2).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_16LE);
        this.fieldB = new String(((MemorySegment) VH_FIELD_B_DATA.get(seg, 0L)).reinterpret((long) VH_FIELD_B_LEN.get(seg, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
        this.fieldC = new String(((MemorySegment) VH_FIELD_C_DATA.get(seg, 0L)).reinterpret((long) VH_FIELD_C_LEN.get(seg, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
    }

    public static BorrowedFieldsWithBounds fromFooAndStrings(Foo foo, String dstr16X, String utf8StrZ) {
        try (var arena = Arena.ofConfined()) {
            char[] dstr16XChars = dstr16X.toCharArray();

            var dstr16XSeg = arena.allocateFrom(ValueLayout.JAVA_CHAR, dstr16XChars);
            byte[] utf8StrZBytes = utf8StrZ.getBytes(StandardCharsets.UTF_8);

            var utf8StrZSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, utf8StrZBytes);
            var dstr16XSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(dstr16XSlice, 0L, dstr16XSeg);
            DiplomatLib.VH_SV_LEN.set(dstr16XSlice, 0L, (long) dstr16XChars.length);
            var utf8StrZSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(utf8StrZSlice, 0L, utf8StrZSeg);
            DiplomatLib.VH_SV_LEN.set(utf8StrZSlice, 0L, (long) utf8StrZBytes.length);
            return BorrowedFieldsWithBounds.fromNative((MemorySegment) BORROWEDFIELDSWITHBOUNDS_FROM_FOO_AND_STRINGS.invokeExact((SegmentAllocator) arena, foo.handle, dstr16XSlice, utf8StrZSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}