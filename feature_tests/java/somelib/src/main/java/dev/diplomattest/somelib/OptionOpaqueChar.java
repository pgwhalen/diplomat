package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class OptionOpaqueChar implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPTIONOPAQUECHAR_ASSERT_CHAR;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaqueChar_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPTIONOPAQUECHAR_ASSERT_CHAR = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionOpaqueChar_assert_char").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
    }

    final MemorySegment handle;

    OptionOpaqueChar(MemorySegment handle) {
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

    public void assertChar(int ch) {
        try {
            OPTIONOPAQUECHAR_ASSERT_CHAR.invokeExact(handle, ch);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}