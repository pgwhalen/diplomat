package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

/**
 * An  data provider, capable of loading  data keys from some source.
 *
 * See the [Rust documentation for `icu_provider`](https://docs.rs/icu_provider/latest/icu_provider/index.html) for more information.
 */
public class DataProvider implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle ICU4X_DATAPROVIDER_NEW_STATIC_MV1;
    private static final MethodHandle ICU4X_DATAPROVIDER_RETURNS_RESULT_MV1;
    static final StructLayout ICU4X_DATAPROVIDER_RETURNS_RESULT_MV1_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_BOOLEAN.withName("is_ok")
        );

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("icu4x_DataProvider_destroy_mv1").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        ICU4X_DATAPROVIDER_NEW_STATIC_MV1 = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("icu4x_DataProvider_new_static_mv1").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
        ICU4X_DATAPROVIDER_RETURNS_RESULT_MV1 = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("icu4x_DataProvider_returns_result_mv1").orElseThrow(),
            FunctionDescriptor.of(ICU4X_DATAPROVIDER_RETURNS_RESULT_MV1_RESULT)
        );
    }

    final MemorySegment handle;

    DataProvider(MemorySegment handle) {
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

    /**
     * See the [Rust documentation for `get_static_provider`](https://docs.rs/icu_testdata/latest/icu_testdata/fn.get_static_provider.html) for more information.
     */
    public static DataProvider static_() {
        try {
            return new DataProvider((MemorySegment) ICU4X_DATAPROVIDER_NEW_STATIC_MV1.invokeExact());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * This exists as a regression test for https://github.com/rust-diplomat/diplomat/issues/155
     */
    public static void returnsResult() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) ICU4X_DATAPROVIDER_RETURNS_RESULT_MV1.invokeExact((SegmentAllocator) arena);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 0L);
            if (isOk) {
                return;
            } else {
                throw new RuntimeException("Diplomat error");
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}