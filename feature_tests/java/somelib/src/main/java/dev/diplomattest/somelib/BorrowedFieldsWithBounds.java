package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class BorrowedFieldsWithBounds {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("fieldA"),
        ValueLayout.JAVA_BYTE.withName("fieldB"),
        ValueLayout.JAVA_BYTE.withName("fieldC")
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

    public Object fieldA;

    public Object fieldB;

    public Object fieldC;

    public BorrowedFieldsWithBounds() {
    }

    BorrowedFieldsWithBounds(Object fieldA, Object fieldB, Object fieldC) {
        this.fieldA = fieldA;
        this.fieldB = fieldB;
        this.fieldC = fieldC;
    }

    static BorrowedFieldsWithBounds fromNative(MemorySegment seg) {
        var result = new BorrowedFieldsWithBounds();
        result.fieldA = null /* unsupported field fieldA */;
        result.fieldB = null /* unsupported field fieldB */;
        result.fieldC = null /* unsupported field fieldC */;
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        // unsupported field fieldA
        // unsupported field fieldB
        // unsupported field fieldC
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