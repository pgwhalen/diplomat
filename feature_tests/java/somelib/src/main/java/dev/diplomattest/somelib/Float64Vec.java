package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Float64Vec implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle FLOAT64VEC_TO_STRING;
    private static final MethodHandle FLOAT64VEC_GET;
    static final StructLayout FLOAT64VEC_GET_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_DOUBLE.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("Float64Vec_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        FLOAT64VEC_TO_STRING = LINKER.downcallHandle(
            LIB.find("Float64Vec_to_string").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        FLOAT64VEC_GET = LINKER.downcallHandle(
            LIB.find("Float64Vec_get").orElseThrow(),
            FunctionDescriptor.of(FLOAT64VEC_GET_RESULT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    final MemorySegment handle;

    Float64Vec(MemorySegment handle) {
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

    public String toString() {
        var write = DiplomatLib.createWrite();
        try {
            FLOAT64VEC_TO_STRING.invokeExact(handle, write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public Optional<Double> get(long i) {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) FLOAT64VEC_GET.invokeExact((SegmentAllocator) arena, handle, i);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                return Optional.of((double) result.get(ValueLayout.JAVA_DOUBLE, 0L));
            } else {
                return Optional.empty();
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}