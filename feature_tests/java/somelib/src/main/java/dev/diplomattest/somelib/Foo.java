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
    }

    final MemorySegment handle;

    Foo(MemorySegment handle) {
        this.handle = handle;
    }

    @Override
    public void close() {
        try {
            DESTROY.invokeExact(handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Foo new_(String x) {
        try (var arena = Arena.ofConfined()) {
            byte[] xBytes = x.getBytes(StandardCharsets.UTF_8);

            var xSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, xBytes);
            return new Foo((MemorySegment) FOO_NEW.invokeExact(xSeg, (long) xBytes.length));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Bar getBar() {
        try {
            return new Bar((MemorySegment) FOO_GET_BAR.invokeExact(handle));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}