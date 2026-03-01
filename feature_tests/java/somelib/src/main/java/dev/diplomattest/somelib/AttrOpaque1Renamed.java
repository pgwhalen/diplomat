package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class AttrOpaque1Renamed implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_ATTROPAQUE1_NEW;
    private static final MethodHandle NAMESPACE_ATTROPAQUE1_MAC_TEST;
    private static final MethodHandle NAMESPACE_ATTROPAQUE1_HELLO;
    private static final MethodHandle NAMESPACE_ATTROPAQUE1_METHOD;
    private static final MethodHandle RENAMED_ON_ABI_ONLY;
    private static final MethodHandle NAMESPACE_ATTROPAQUE1_USE_UNNAMESPACED;
    private static final MethodHandle NAMESPACE_ATTROPAQUE1_USE_NAMESPACED;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("namespace_AttrOpaque1_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_ATTROPAQUE1_NEW = LINKER.downcallHandle(
            LIB.find("namespace_AttrOpaque1_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
        NAMESPACE_ATTROPAQUE1_MAC_TEST = LINKER.downcallHandle(
            LIB.find("namespace_AttrOpaque1_mac_test").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT)
        );
        NAMESPACE_ATTROPAQUE1_HELLO = LINKER.downcallHandle(
            LIB.find("namespace_AttrOpaque1_hello").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT)
        );
        NAMESPACE_ATTROPAQUE1_METHOD = LINKER.downcallHandle(
            LIB.find("namespace_AttrOpaque1_method").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_BYTE, ValueLayout.ADDRESS)
        );
        RENAMED_ON_ABI_ONLY = LINKER.downcallHandle(
            LIB.find("renamed_on_abi_only").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_BYTE, ValueLayout.ADDRESS)
        );
        NAMESPACE_ATTROPAQUE1_USE_UNNAMESPACED = LINKER.downcallHandle(
            LIB.find("namespace_AttrOpaque1_use_unnamespaced").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        NAMESPACE_ATTROPAQUE1_USE_NAMESPACED = LINKER.downcallHandle(
            LIB.find("namespace_AttrOpaque1_use_namespaced").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
    }

    final MemorySegment handle;

    AttrOpaque1Renamed(MemorySegment handle) {
        this.handle = handle;
    }

    public AttrOpaque1Renamed() {
        try {
            this.handle = (MemorySegment) NAMESPACE_ATTROPAQUE1_NEW.invokeExact();
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

    public static int macTest() {
        try {
            return (int) NAMESPACE_ATTROPAQUE1_MAC_TEST.invokeExact();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int hello() {
        try {
            return (int) NAMESPACE_ATTROPAQUE1_HELLO.invokeExact();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public byte method_renamed() {
        try {
            return (byte) NAMESPACE_ATTROPAQUE1_METHOD.invokeExact(handle);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public byte abirenamed() {
        try {
            return (byte) RENAMED_ON_ABI_ONLY.invokeExact(handle);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void useUnnamespaced(Unnamespaced un) {
        try {
            NAMESPACE_ATTROPAQUE1_USE_UNNAMESPACED.invokeExact(handle, un.handle);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void useNamespaced(RenamedAttrEnum n) {
        try {
            NAMESPACE_ATTROPAQUE1_USE_NAMESPACED.invokeExact(handle, n.toNative());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}