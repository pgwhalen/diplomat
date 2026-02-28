package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class OptionString implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPTIONSTRING_NEW;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("OptionString_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPTIONSTRING_NEW = LINKER.downcallHandle(
            LIB.find("OptionString_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    final MemorySegment handle;

    OptionString(MemorySegment handle) {
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

    public static Optional<OptionString> new_(String diplomatStr) {
        try (var arena = Arena.ofConfined()) {
            byte[] diplomatStrBytes = diplomatStr.getBytes(StandardCharsets.UTF_8);

            var diplomatStrSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, diplomatStrBytes);
            var resultAddr = (MemorySegment) OPTIONSTRING_NEW.invokeExact(diplomatStrSeg, (long) diplomatStrBytes.length);
            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new OptionString(resultAddr));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}