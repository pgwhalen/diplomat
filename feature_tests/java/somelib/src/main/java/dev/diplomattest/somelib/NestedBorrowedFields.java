package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class NestedBorrowedFields {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        BorrowedFields.LAYOUT.withName("fields"),
        BorrowedFieldsWithBounds.LAYOUT.withName("bounds"),
        BorrowedFieldsWithBounds.LAYOUT.withName("bounds2")
    );

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle NESTEDBORROWEDFIELDS_FROM_BAR_AND_FOO_AND_STRINGS;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        NESTEDBORROWEDFIELDS_FROM_BAR_AND_FOO_AND_STRINGS = LINKER.downcallHandle(
            LIB.find("NestedBorrowedFields_from_bar_and_foo_and_strings").orElseThrow(),
            FunctionDescriptor.of(NestedBorrowedFields.LAYOUT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
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
        var result = new NestedBorrowedFields();
        result.fields = BorrowedFields.fromNative(seg.asSlice(0L, BorrowedFields.LAYOUT.byteSize()));
        result.bounds = BorrowedFieldsWithBounds.fromNative(seg.asSlice(48L, BorrowedFieldsWithBounds.LAYOUT.byteSize()));
        result.bounds2 = BorrowedFieldsWithBounds.fromNative(seg.asSlice(96L, BorrowedFieldsWithBounds.LAYOUT.byteSize()));
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.asSlice(0L, BorrowedFields.LAYOUT.byteSize()).copyFrom(this.fields.toNative(arena));
        seg.asSlice(48L, BorrowedFieldsWithBounds.LAYOUT.byteSize()).copyFrom(this.bounds.toNative(arena));
        seg.asSlice(96L, BorrowedFieldsWithBounds.LAYOUT.byteSize()).copyFrom(this.bounds2.toNative(arena));
        return seg;
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
            return NestedBorrowedFields.fromNative((MemorySegment) NESTEDBORROWEDFIELDS_FROM_BAR_AND_FOO_AND_STRINGS.invokeExact((SegmentAllocator) arena, bar.handle, foo.handle, dstr16XSeg, (long) dstr16XChars.length, dstr16ZSeg, (long) dstr16ZChars.length, utf8StrYSeg, (long) utf8StrYBytes.length, utf8StrZSeg, (long) utf8StrZBytes.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}