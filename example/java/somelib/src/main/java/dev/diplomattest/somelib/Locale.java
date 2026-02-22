package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class Locale implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle ICU4X_LOCALE_NEW_MV1;

    static {
        System.loadLibrary("diplomat_example");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("icu4x_Locale_destroy_mv1").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        ICU4X_LOCALE_NEW_MV1 = LINKER.downcallHandle(
            LIB.find("icu4x_Locale_new_mv1").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    final MemorySegment handle;

    Locale(MemorySegment handle) {
        this.handle = handle;
    }

    @Override
    public void close() {
        try { DESTROY.invokeExact(handle); }
        catch (Throwable e) { throw new RuntimeException(e); }
    }

    public static Locale new_(String name) {
        try (var arena = Arena.ofConfined()) {
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

            var nameSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, nameBytes);

            return new Locale((MemorySegment) ICU4X_LOCALE_NEW_MV1.invokeExact(nameSeg, (long) nameBytes.length));

        } catch (Throwable e) { throw new RuntimeException(e); }
    }
}