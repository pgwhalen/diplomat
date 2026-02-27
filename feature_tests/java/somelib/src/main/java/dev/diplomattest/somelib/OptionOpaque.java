package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class OptionOpaque implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPTIONOPAQUE_NEW;
    private static final MethodHandle OPTIONOPAQUE_NEW_NONE;
    private static final MethodHandle OPTIONOPAQUE_RETURNS_NONE_SELF;
    private static final MethodHandle OPTIONOPAQUE_RETURNS_SOME_SELF;
    private static final MethodHandle OPTIONOPAQUE_ASSERT_INTEGER;
    private static final MethodHandle OPTIONOPAQUE_OPTION_OPAQUE_ARGUMENT;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("OptionOpaque_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_NEW = LINKER.downcallHandle(
            LIB.find("OptionOpaque_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
        OPTIONOPAQUE_NEW_NONE = LINKER.downcallHandle(
            LIB.find("OptionOpaque_new_none").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_RETURNS_NONE_SELF = LINKER.downcallHandle(
            LIB.find("OptionOpaque_returns_none_self").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_RETURNS_SOME_SELF = LINKER.downcallHandle(
            LIB.find("OptionOpaque_returns_some_self").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        OPTIONOPAQUE_ASSERT_INTEGER = LINKER.downcallHandle(
            LIB.find("OptionOpaque_assert_integer").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
        OPTIONOPAQUE_OPTION_OPAQUE_ARGUMENT = LINKER.downcallHandle(
            LIB.find("OptionOpaque_option_opaque_argument").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_BYTE, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    OptionOpaque(MemorySegment handle) {
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

    public static OptionOpaque new_(int i) {
        try {
            return new OptionOpaque((MemorySegment) OPTIONOPAQUE_NEW.invokeExact(i));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static OptionOpaque newNone() {
        try {
            return new OptionOpaque((MemorySegment) OPTIONOPAQUE_NEW_NONE.invokeExact());
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean optionOpaqueArgument(OptionOpaque arg) {
        try {
            return (boolean) OPTIONOPAQUE_OPTION_OPAQUE_ARGUMENT.invokeExact(arg.handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public OptionOpaque returnsNoneSelf() {
        try {
            return new OptionOpaque((MemorySegment) OPTIONOPAQUE_RETURNS_NONE_SELF.invokeExact(handle));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public OptionOpaque returnsSomeSelf() {
        try {
            return new OptionOpaque((MemorySegment) OPTIONOPAQUE_RETURNS_SOME_SELF.invokeExact(handle));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void assertInteger(int i) {
        try {
            OPTIONOPAQUE_ASSERT_INTEGER.invokeExact(handle, i);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}