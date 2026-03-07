package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class NestedBorrowedFields {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        BorrowedFields.LAYOUT.withName("fields"),
        BorrowedFieldsWithBounds.LAYOUT.withName("bounds"),
        BorrowedFieldsWithBounds.LAYOUT.withName("bounds2")
    );
    private static final long OFFSET_FIELDS = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("fields"));
    private static final long OFFSET_BOUNDS = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("bounds"));
    private static final long OFFSET_BOUNDS2 = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("bounds2"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle NESTEDBORROWEDFIELDS_FROM_BAR_AND_FOO_AND_STRINGS;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        NESTEDBORROWEDFIELDS_FROM_BAR_AND_FOO_AND_STRINGS = LINKER.downcallHandle(
            LIB.find("NestedBorrowedFields_from_bar_and_foo_and_strings").orElseThrow(),
            FunctionDescriptor.of(NestedBorrowedFields.LAYOUT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW, DiplomatLib.DIPLOMAT_STRING_VIEW, DiplomatLib.DIPLOMAT_STRING_VIEW, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
    }

    public BorrowedFields fields;

    public BorrowedFieldsWithBounds bounds;

    public BorrowedFieldsWithBounds bounds2;

    public NestedBorrowedFields() {
    }

    NestedBorrowedFields(BorrowedFields fields, BorrowedFieldsWithBounds bounds, BorrowedFieldsWithBounds bounds2) {
        this.fields = fields;
        this.bounds = bounds;
        this.bounds2 = bounds2;
    }

    static NestedBorrowedFields fromNative(MemorySegment seg) {
        return new NestedBorrowedFields(
            BorrowedFields.fromNative(seg.asSlice(OFFSET_FIELDS, BorrowedFields.LAYOUT.byteSize())),
            BorrowedFieldsWithBounds.fromNative(seg.asSlice(OFFSET_BOUNDS, BorrowedFieldsWithBounds.LAYOUT.byteSize())),
            BorrowedFieldsWithBounds.fromNative(seg.asSlice(OFFSET_BOUNDS2, BorrowedFieldsWithBounds.LAYOUT.byteSize()))
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.asSlice(OFFSET_FIELDS, BorrowedFields.LAYOUT.byteSize()).copyFrom(this.fields.toNative(arena));
        seg.asSlice(OFFSET_BOUNDS, BorrowedFieldsWithBounds.LAYOUT.byteSize()).copyFrom(this.bounds.toNative(arena));
        seg.asSlice(OFFSET_BOUNDS2, BorrowedFieldsWithBounds.LAYOUT.byteSize()).copyFrom(this.bounds2.toNative(arena));
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.fields = BorrowedFields.fromNative(seg.asSlice(OFFSET_FIELDS, BorrowedFields.LAYOUT.byteSize()));
        this.bounds = BorrowedFieldsWithBounds.fromNative(seg.asSlice(OFFSET_BOUNDS, BorrowedFieldsWithBounds.LAYOUT.byteSize()));
        this.bounds2 = BorrowedFieldsWithBounds.fromNative(seg.asSlice(OFFSET_BOUNDS2, BorrowedFieldsWithBounds.LAYOUT.byteSize()));
    }

    public static NestedBorrowedFields fromBarAndFooAndStrings(Bar bar, Foo foo, String dstr16X, String dstr16Z, String utf8StrY, String utf8StrZ) {
        try (var arena = Arena.ofConfined()) {
            char[] dstr16XChars = dstr16X.toCharArray();

            var dstr16XSeg = arena.allocateFrom(ValueLayout.JAVA_CHAR, dstr16XChars);
            char[] dstr16ZChars = dstr16Z.toCharArray();

            var dstr16ZSeg = arena.allocateFrom(ValueLayout.JAVA_CHAR, dstr16ZChars);
            byte[] utf8StrYBytes = utf8StrY.getBytes(StandardCharsets.UTF_8);

            var utf8StrYSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, utf8StrYBytes);
            byte[] utf8StrZBytes = utf8StrZ.getBytes(StandardCharsets.UTF_8);

            var utf8StrZSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, utf8StrZBytes);
            var dstr16XSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(dstr16XSlice, 0L, dstr16XSeg);
            DiplomatLib.VH_SV_LEN.set(dstr16XSlice, 0L, (long) dstr16XChars.length);
            var dstr16ZSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(dstr16ZSlice, 0L, dstr16ZSeg);
            DiplomatLib.VH_SV_LEN.set(dstr16ZSlice, 0L, (long) dstr16ZChars.length);
            var utf8StrYSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(utf8StrYSlice, 0L, utf8StrYSeg);
            DiplomatLib.VH_SV_LEN.set(utf8StrYSlice, 0L, (long) utf8StrYBytes.length);
            var utf8StrZSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(utf8StrZSlice, 0L, utf8StrZSeg);
            DiplomatLib.VH_SV_LEN.set(utf8StrZSlice, 0L, (long) utf8StrZBytes.length);
            return NestedBorrowedFields.fromNative((MemorySegment) NESTEDBORROWEDFIELDS_FROM_BAR_AND_FOO_AND_STRINGS.invokeExact((SegmentAllocator) arena, bar.handle, foo.handle, dstr16XSlice, dstr16ZSlice, utf8StrYSlice, utf8StrZSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}