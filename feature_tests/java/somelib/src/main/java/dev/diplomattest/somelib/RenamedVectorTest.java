package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class RenamedVectorTest implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_VECTORTEST_NEW;
    private static final MethodHandle NAMESPACE_VECTORTEST_LEN;
    private static final MethodHandle NAMESPACE_VECTORTEST_PUSH;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("namespace_VectorTest_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_VECTORTEST_NEW = LINKER.downcallHandle(
            LIB.find("namespace_VectorTest_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
        NAMESPACE_VECTORTEST_LEN = LINKER.downcallHandle(
            LIB.find("namespace_VectorTest_len").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
        );
        NAMESPACE_VECTORTEST_PUSH = LINKER.downcallHandle(
            LIB.find("namespace_VectorTest_push").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE)
        );
    }

    final MemorySegment handle;

    RenamedVectorTest(MemorySegment handle) {
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

    public static RenamedVectorTest new_() {
        try {
            return new RenamedVectorTest((MemorySegment) NAMESPACE_VECTORTEST_NEW.invokeExact());
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public long len() {
        try {
            return (long) NAMESPACE_VECTORTEST_LEN.invokeExact(handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void push(double value) {
        try {
            NAMESPACE_VECTORTEST_PUSH.invokeExact(handle, value);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}