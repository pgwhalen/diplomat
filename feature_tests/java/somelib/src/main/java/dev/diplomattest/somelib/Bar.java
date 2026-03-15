package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class Bar implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle BAR_FOO;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Bar_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        BAR_FOO = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("Bar_foo").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    Bar(MemorySegment handle) {
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

    public Foo foo() {
        try {
            return new Foo((MemorySegment) BAR_FOO.invokeExact(handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}