package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Some example docs
 * Back to all docs
 */
public class AttrOpaque1Renamed implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_ATTROPAQUE1_NEW;
    private static final MethodHandle NAMESPACE_ATTROPAQUE1_TEST_NAMESPACED_CALLBACK;
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
        NAMESPACE_ATTROPAQUE1_TEST_NAMESPACED_CALLBACK = LINKER.downcallHandle(
            LIB.find("namespace_AttrOpaque1_test_namespaced_callback").orElseThrow(),
            FunctionDescriptor.ofVoid(DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT)
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

    @FunctionalInterface
    public interface TestNamespacedCallbackT {
        void invoke();
    }

    private static void runCallback_testNamespacedCallback_t(MemorySegment data) {
        @SuppressWarnings("unchecked")
        TestNamespacedCallbackT cb = DiplomatLib.getCallback(data.address(), TestNamespacedCallbackT.class);
        cb.invoke();
    }

    private static final MethodHandle MH_RUN_testNamespacedCallback_t;
    private static final MemorySegment UPCALL_testNamespacedCallback_t;
    static {
        try {
            MH_RUN_testNamespacedCallback_t = MethodHandles.lookup().findStatic(
                AttrOpaque1Renamed.class, "runCallback_testNamespacedCallback_t",
                MethodType.methodType(void.class, MemorySegment.class));
            UPCALL_testNamespacedCallback_t = DiplomatLib.LINKER_SHARED.upcallStub(
                MH_RUN_testNamespacedCallback_t,
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                Arena.global());
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    final MemorySegment handle;

    AttrOpaque1Renamed(MemorySegment handle) {
        this.handle = handle;
    }

    /**
     * More example docs
     */
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

    public static void testNamespacedCallback(TestNamespacedCallbackT t) {
        try (var arena = Arena.ofConfined()) {
            long tId = DiplomatLib.registerCallback(t);
            var tNative = arena.allocate(DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT);
            DiplomatLib.VH_CB_DATA.set(tNative, 0L, MemorySegment.ofAddress(tId));
            DiplomatLib.VH_CB_RUN.set(tNative, 0L, UPCALL_testNamespacedCallback_t);
            DiplomatLib.VH_CB_DESTRUCTOR.set(tNative, 0L, DiplomatLib.DESTRUCTOR_STUB);
            NAMESPACE_ATTROPAQUE1_TEST_NAMESPACED_CALLBACK.invokeExact(tNative);
        } catch (RuntimeException ex) {
            throw ex;
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