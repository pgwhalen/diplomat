package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class FixedDecimalFormatter implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle ICU4X_FIXEDDECIMALFORMATTER_TRY_NEW_MV1;
    private static final MethodHandle ICU4X_FIXEDDECIMALFORMATTER_FORMAT_WRITE_MV1;
    static final StructLayout ICU4X_FIXEDDECIMALFORMATTER_TRY_NEW_MV1_RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );

    static {
        System.loadLibrary("diplomat_example");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("icu4x_FixedDecimalFormatter_destroy_mv1").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        ICU4X_FIXEDDECIMALFORMATTER_TRY_NEW_MV1 = LINKER.downcallHandle(
            LIB.find("icu4x_FixedDecimalFormatter_try_new_mv1").orElseThrow(),
            FunctionDescriptor.of(ICU4X_FIXEDDECIMALFORMATTER_TRY_NEW_MV1_RESULT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, FixedDecimalFormatterOptions.LAYOUT)
        );
        ICU4X_FIXEDDECIMALFORMATTER_FORMAT_WRITE_MV1 = LINKER.downcallHandle(
            LIB.find("icu4x_FixedDecimalFormatter_format_write_mv1").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    FixedDecimalFormatter(MemorySegment handle) {
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

    public static FixedDecimalFormatter tryNew(Locale locale, DataProvider provider, FixedDecimalFormatterOptions options) {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) ICU4X_FIXEDDECIMALFORMATTER_TRY_NEW_MV1.invokeExact((SegmentAllocator) arena, locale.handle, provider.handle, options.toNative(arena));
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return new FixedDecimalFormatter(result.get(ValueLayout.ADDRESS, 0L));
            } else {
                throw new RuntimeException("Diplomat error");
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String formatWrite(FixedDecimal value) {
        var write = DiplomatLib.createWrite();
        try {
            ICU4X_FIXEDDECIMALFORMATTER_FORMAT_WRITE_MV1.invokeExact(handle, value.handle, write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}