package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class Unnamespaced implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_UNNAMESPACED_MAKE;
    private static final MethodHandle NAMESPACE_UNNAMESPACED_USE_NAMESPACED;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_Unnamespaced_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_UNNAMESPACED_MAKE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_Unnamespaced_make").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
        NAMESPACE_UNNAMESPACED_USE_NAMESPACED = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_Unnamespaced_use_namespaced").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    Unnamespaced(MemorySegment handle) {
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

    public static Unnamespaced make(RenamedAttrEnum e) {
        try {
            return new Unnamespaced((MemorySegment) NAMESPACE_UNNAMESPACED_MAKE.invokeExact(e.toNative()));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void useNamespaced(AttrOpaque1Renamed n) {
        try {
            NAMESPACE_UNNAMESPACED_USE_NAMESPACED.invokeExact(handle, n.handle);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}