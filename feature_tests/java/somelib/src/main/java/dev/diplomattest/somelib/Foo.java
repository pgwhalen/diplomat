package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class Foo implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle FOO_NEW;
    private static final MethodHandle FOO_GET_BAR;
    private static final MethodHandle FOO_AS_RETURNING;
    private static final MethodHandle FOO_EXTRACT_FROM_FIELDS;
    private static final MethodHandle FOO_EXTRACT_FROM_BOUNDS;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("Foo_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        FOO_NEW = LINKER.downcallHandle(
            LIB.find("Foo_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        FOO_GET_BAR = LINKER.downcallHandle(
            LIB.find("Foo_get_bar").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        FOO_AS_RETURNING = LINKER.downcallHandle(
            LIB.find("Foo_as_returning").orElseThrow(),
            FunctionDescriptor.of(BorrowedFieldsReturning.LAYOUT, ValueLayout.ADDRESS)
        );
        FOO_EXTRACT_FROM_FIELDS = LINKER.downcallHandle(
            LIB.find("Foo_extract_from_fields").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, BorrowedFields.LAYOUT)
        );
        FOO_EXTRACT_FROM_BOUNDS = LINKER.downcallHandle(
            LIB.find("Foo_extract_from_bounds").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, BorrowedFieldsWithBounds.LAYOUT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    final MemorySegment handle;

    Foo(MemorySegment handle) {
        this.handle = handle;
    }

    public Foo(String x) {
        try (var arena = Arena.ofConfined()) {
            byte[] xBytes = x.getBytes(StandardCharsets.UTF_8);

            var xSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, xBytes);
            this.handle = (MemorySegment) FOO_NEW.invokeExact(xSeg, (long) xBytes.length);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() {
        try {
            DESTROY.invokeExact(handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Foo extractFromFields(BorrowedFields fields) {
        try (var arena = Arena.ofConfined()) {
            return new Foo((MemorySegment) FOO_EXTRACT_FROM_FIELDS.invokeExact(fields.toNative(arena)));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Foo extractFromBounds(BorrowedFieldsWithBounds bounds, String anotherString) {
        try (var arena = Arena.ofConfined()) {
            byte[] anotherStringBytes = anotherString.getBytes(StandardCharsets.UTF_8);

            var anotherStringSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, anotherStringBytes);
            return new Foo((MemorySegment) FOO_EXTRACT_FROM_BOUNDS.invokeExact(bounds.toNative(arena), anotherStringSeg, (long) anotherStringBytes.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Bar getBar() {
        try {
            return new Bar((MemorySegment) FOO_GET_BAR.invokeExact(handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public BorrowedFieldsReturning asReturning() {
        try (var arena = Arena.ofConfined()) {
            return BorrowedFieldsReturning.fromNative((MemorySegment) FOO_AS_RETURNING.invokeExact((SegmentAllocator) arena, handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}