package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

/**
 * An  Locale, capable of representing strings like `"en-US"`.
 *
 * See the [Rust documentation for `Locale`](https://docs.rs/icu/latest/icu/locid/struct.Locale.html) for more information.
 */
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

    /**
     * Construct an {@link Locale} from a locale identifier represented as a string.
     */
    public Locale(String name) {
        try (var arena = Arena.ofConfined()) {
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

            var nameSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, nameBytes);
            this.handle = (MemorySegment) ICU4X_LOCALE_NEW_MV1.invokeExact(nameSeg, (long) nameBytes.length);
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
}