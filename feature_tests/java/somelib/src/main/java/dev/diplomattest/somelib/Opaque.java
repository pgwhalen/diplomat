package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Opaque implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPAQUE_NEW;
    private static final MethodHandle OPAQUE_TRY_FROM_UTF8;
    private static final MethodHandle OPAQUE_FROM_STR;
    private static final MethodHandle OPAQUE_GET_DEBUG_STR;
    private static final MethodHandle OPAQUE_ASSERT_STRUCT;
    private static final MethodHandle OPAQUE_RETURNS_USIZE;
    private static final MethodHandle OPAQUE_RETURNS_IMPORTED;
    private static final MethodHandle OPAQUE_CMP;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("Opaque_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPAQUE_NEW = LINKER.downcallHandle(
            LIB.find("Opaque_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
        OPAQUE_TRY_FROM_UTF8 = LINKER.downcallHandle(
            LIB.find("Opaque_try_from_utf8").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        OPAQUE_FROM_STR = LINKER.downcallHandle(
            LIB.find("Opaque_from_str").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        OPAQUE_GET_DEBUG_STR = LINKER.downcallHandle(
            LIB.find("Opaque_get_debug_str").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        OPAQUE_ASSERT_STRUCT = LINKER.downcallHandle(
            LIB.find("Opaque_assert_struct").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, MyStruct.LAYOUT)
        );
        OPAQUE_RETURNS_USIZE = LINKER.downcallHandle(
            LIB.find("Opaque_returns_usize").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG)
        );
        OPAQUE_RETURNS_IMPORTED = LINKER.downcallHandle(
            LIB.find("Opaque_returns_imported").orElseThrow(),
            FunctionDescriptor.of(ImportedStruct.LAYOUT)
        );
        OPAQUE_CMP = LINKER.downcallHandle(
            LIB.find("Opaque_cmp").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_BYTE)
        );
    }

    final MemorySegment handle;

    Opaque(MemorySegment handle) {
        this.handle = handle;
    }

    public Opaque() {
        try {
            this.handle = (MemorySegment) OPAQUE_NEW.invokeExact();
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

    public static Optional<Opaque> tryFromUtf8(String input) {
        try (var arena = Arena.ofConfined()) {
            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

            var inputSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, inputBytes);
            var resultAddr = (MemorySegment) OPAQUE_TRY_FROM_UTF8.invokeExact(inputSeg, (long) inputBytes.length);
            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new Opaque(resultAddr));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Opaque fromStr(String input) {
        try (var arena = Arena.ofConfined()) {
            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

            var inputSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, inputBytes);
            return new Opaque((MemorySegment) OPAQUE_FROM_STR.invokeExact(inputSeg, (long) inputBytes.length));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static long returnsUsize() {
        try {
            return (long) OPAQUE_RETURNS_USIZE.invokeExact();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static ImportedStruct returnsImported() {
        try (var arena = Arena.ofConfined()) {
            return ImportedStruct.fromNative((MemorySegment) OPAQUE_RETURNS_IMPORTED.invokeExact((SegmentAllocator) arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte cmp() {
        try {
            return (byte) OPAQUE_CMP.invokeExact();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getDebugStr() {
        var write = DiplomatLib.createWrite();
        try {
            OPAQUE_GET_DEBUG_STR.invokeExact(handle, write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void assertStruct(MyStruct s) {
        try (var arena = Arena.ofConfined()) {
            OPAQUE_ASSERT_STRUCT.invokeExact(handle, s.toNative(arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}