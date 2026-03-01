package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class BorrowedFieldsWithBounds {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("fieldA"),
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("fieldB"),
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("fieldC")
    );

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle BORROWEDFIELDSWITHBOUNDS_FROM_FOO_AND_STRINGS;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        BORROWEDFIELDSWITHBOUNDS_FROM_FOO_AND_STRINGS = LINKER.downcallHandle(
            LIB.find("BorrowedFieldsWithBounds_from_foo_and_strings").orElseThrow(),
            FunctionDescriptor.of(BorrowedFieldsWithBounds.LAYOUT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
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
            new String(seg.get(ValueLayout.ADDRESS, 0L).reinterpret(seg.get(ValueLayout.JAVA_LONG, 8L) * 2).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_16LE),
            new String(seg.get(ValueLayout.ADDRESS, 16L).reinterpret(seg.get(ValueLayout.JAVA_LONG, 24L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8),
            new String(seg.get(ValueLayout.ADDRESS, 32L).reinterpret(seg.get(ValueLayout.JAVA_LONG, 40L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        { byte[] fieldABytes = this.fieldA.getBytes(StandardCharsets.UTF_16LE); var fieldASeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, fieldABytes); seg.set(ValueLayout.ADDRESS, 0L, fieldASeg); seg.set(ValueLayout.JAVA_LONG, 8L, (long) fieldABytes.length / 2); }
        { byte[] fieldBBytes = this.fieldB.getBytes(StandardCharsets.UTF_8); var fieldBSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, fieldBBytes); seg.set(ValueLayout.ADDRESS, 16L, fieldBSeg); seg.set(ValueLayout.JAVA_LONG, 24L, (long) fieldBBytes.length); }
        { byte[] fieldCBytes = this.fieldC.getBytes(StandardCharsets.UTF_8); var fieldCSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, fieldCBytes); seg.set(ValueLayout.ADDRESS, 32L, fieldCSeg); seg.set(ValueLayout.JAVA_LONG, 40L, (long) fieldCBytes.length); }
        return seg;
    }

    public static BorrowedFieldsWithBounds fromFooAndStrings(Foo foo, String dstr16X, String utf8StrZ) {
        try (var arena = Arena.ofConfined()) {
            char[] dstr16XChars = dstr16X.toCharArray();

            var dstr16XSeg = arena.allocateFrom(ValueLayout.JAVA_CHAR, dstr16XChars);
            byte[] utf8StrZBytes = utf8StrZ.getBytes(StandardCharsets.UTF_8);

            var utf8StrZSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, utf8StrZBytes);
            return BorrowedFieldsWithBounds.fromNative((MemorySegment) BORROWEDFIELDSWITHBOUNDS_FROM_FOO_AND_STRINGS.invokeExact((SegmentAllocator) arena, foo.handle, dstr16XSeg, (long) dstr16XChars.length, utf8StrZSeg, (long) utf8StrZBytes.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}