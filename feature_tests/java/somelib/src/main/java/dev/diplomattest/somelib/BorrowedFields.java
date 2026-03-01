package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class BorrowedFields {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("a"),
        ValueLayout.JAVA_BYTE.withName("b"),
        ValueLayout.JAVA_BYTE.withName("c")
    );

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

    public Object a;

    public Object b;

    public Object c;

    public BorrowedFields() {
    }

    BorrowedFields(Object a, Object b, Object c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    static BorrowedFields fromNative(MemorySegment seg) {
        var result = new BorrowedFields();
        result.a = null /* unsupported field a */;
        result.b = null /* unsupported field b */;
        result.c = null /* unsupported field c */;
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        // unsupported field a
        // unsupported field b
        // unsupported field c
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