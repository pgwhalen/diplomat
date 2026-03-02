package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class CallbackWrapper {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BOOLEAN.withName("cantBeEmpty")
    );
    private static final VarHandle VH_CANT_BE_EMPTY = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("cantBeEmpty"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle CALLBACKWRAPPER_TEST_MULTI_ARG_CALLBACK;
    private static final MethodHandle CALLBACKWRAPPER_TEST_NO_ARGS;
    private static final MethodHandle CALLBACKWRAPPER_TEST_CB_WITH_STRUCT;
    private static final MethodHandle CALLBACKWRAPPER_TEST_MULTIPLE_CB_ARGS;
    private static final MethodHandle CALLBACKWRAPPER_TEST_STR_CB_ARG;
    private static final MethodHandle CALLBACKWRAPPER_TEST_SLICE_CB_ARG;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        CALLBACKWRAPPER_TEST_MULTI_ARG_CALLBACK = LINKER.downcallHandle(
            LIB.find("CallbackWrapper_test_multi_arg_callback").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT, ValueLayout.JAVA_INT)
        );
        CALLBACKWRAPPER_TEST_NO_ARGS = LINKER.downcallHandle(
            LIB.find("CallbackWrapper_test_no_args").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT)
        );
        CALLBACKWRAPPER_TEST_CB_WITH_STRUCT = LINKER.downcallHandle(
            LIB.find("CallbackWrapper_test_cb_with_struct").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT)
        );
        CALLBACKWRAPPER_TEST_MULTIPLE_CB_ARGS = LINKER.downcallHandle(
            LIB.find("CallbackWrapper_test_multiple_cb_args").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT, DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT)
        );
        CALLBACKWRAPPER_TEST_STR_CB_ARG = LINKER.downcallHandle(
            LIB.find("CallbackWrapper_test_str_cb_arg").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT)
        );
        CALLBACKWRAPPER_TEST_SLICE_CB_ARG = LINKER.downcallHandle(
            LIB.find("CallbackWrapper_test_slice_cb_arg").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT)
        );
    }

    @FunctionalInterface
    public interface TestMultiArgCallbackF {
        int invoke(int arg0);
    }

    @FunctionalInterface
    public interface TestNoArgsH {
        void invoke();
    }

    @FunctionalInterface
    public interface TestCbWithStructF {
        int invoke(CallbackTestingStruct arg0);
    }

    @FunctionalInterface
    public interface TestMultipleCbArgsF {
        int invoke();
    }

    @FunctionalInterface
    public interface TestMultipleCbArgsG {
        int invoke(int arg0);
    }

    @FunctionalInterface
    public interface TestStrCbArgF {
        int invoke(String arg0);
    }

    @FunctionalInterface
    public interface TestSliceCbArgF {
        void invoke(byte[] arg0);
    }

    private static int runCallback_testMultiArgCallback_f(MemorySegment data, int arg0) {
        @SuppressWarnings("unchecked")
        TestMultiArgCallbackF cb = DiplomatLib.getCallback(data.address(), TestMultiArgCallbackF.class);
        return cb.invoke(arg0);
    }

    private static void runCallback_testNoArgs_h(MemorySegment data) {
        @SuppressWarnings("unchecked")
        TestNoArgsH cb = DiplomatLib.getCallback(data.address(), TestNoArgsH.class);
        cb.invoke();
    }

    private static int runCallback_testCbWithStruct_f(MemorySegment data, MemorySegment arg0) {
        @SuppressWarnings("unchecked")
        TestCbWithStructF cb = DiplomatLib.getCallback(data.address(), TestCbWithStructF.class);
        return cb.invoke(CallbackTestingStruct.fromNative(arg0));
    }

    private static int runCallback_testMultipleCbArgs_f(MemorySegment data) {
        @SuppressWarnings("unchecked")
        TestMultipleCbArgsF cb = DiplomatLib.getCallback(data.address(), TestMultipleCbArgsF.class);
        return cb.invoke();
    }

    private static int runCallback_testMultipleCbArgs_g(MemorySegment data, int arg0) {
        @SuppressWarnings("unchecked")
        TestMultipleCbArgsG cb = DiplomatLib.getCallback(data.address(), TestMultipleCbArgsG.class);
        return cb.invoke(arg0);
    }

    private static int runCallback_testStrCbArg_f(MemorySegment data, MemorySegment arg0) {
        @SuppressWarnings("unchecked")
        TestStrCbArgF cb = DiplomatLib.getCallback(data.address(), TestStrCbArgF.class);
        return cb.invoke(new String(((MemorySegment) DiplomatLib.VH_SV_DATA.get(arg0, 0L)).reinterpret((long) DiplomatLib.VH_SV_LEN.get(arg0, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8));
    }

    private static void runCallback_testSliceCbArg_f(MemorySegment data, MemorySegment arg0) {
        @SuppressWarnings("unchecked")
        TestSliceCbArgF cb = DiplomatLib.getCallback(data.address(), TestSliceCbArgF.class);
        cb.invoke(((MemorySegment) DiplomatLib.VH_SV_DATA.get(arg0, 0L)).reinterpret((long) DiplomatLib.VH_SV_LEN.get(arg0, 0L) * 1L).toArray(ValueLayout.JAVA_BYTE));
    }

    private static final MethodHandle MH_RUN_testMultiArgCallback_f;
    private static final MemorySegment UPCALL_testMultiArgCallback_f;
    static {
        try {
            MH_RUN_testMultiArgCallback_f = MethodHandles.lookup().findStatic(
                CallbackWrapper.class, "runCallback_testMultiArgCallback_f",
                MethodType.methodType(int.class, MemorySegment.class, int.class));
            UPCALL_testMultiArgCallback_f = DiplomatLib.LINKER_SHARED.upcallStub(
                MH_RUN_testMultiArgCallback_f,
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
                Arena.global());
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static final MethodHandle MH_RUN_testNoArgs_h;
    private static final MemorySegment UPCALL_testNoArgs_h;
    static {
        try {
            MH_RUN_testNoArgs_h = MethodHandles.lookup().findStatic(
                CallbackWrapper.class, "runCallback_testNoArgs_h",
                MethodType.methodType(void.class, MemorySegment.class));
            UPCALL_testNoArgs_h = DiplomatLib.LINKER_SHARED.upcallStub(
                MH_RUN_testNoArgs_h,
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                Arena.global());
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static final MethodHandle MH_RUN_testCbWithStruct_f;
    private static final MemorySegment UPCALL_testCbWithStruct_f;
    static {
        try {
            MH_RUN_testCbWithStruct_f = MethodHandles.lookup().findStatic(
                CallbackWrapper.class, "runCallback_testCbWithStruct_f",
                MethodType.methodType(int.class, MemorySegment.class, MemorySegment.class));
            UPCALL_testCbWithStruct_f = DiplomatLib.LINKER_SHARED.upcallStub(
                MH_RUN_testCbWithStruct_f,
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, CallbackTestingStruct.LAYOUT),
                Arena.global());
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static final MethodHandle MH_RUN_testMultipleCbArgs_f;
    private static final MemorySegment UPCALL_testMultipleCbArgs_f;
    static {
        try {
            MH_RUN_testMultipleCbArgs_f = MethodHandles.lookup().findStatic(
                CallbackWrapper.class, "runCallback_testMultipleCbArgs_f",
                MethodType.methodType(int.class, MemorySegment.class));
            UPCALL_testMultipleCbArgs_f = DiplomatLib.LINKER_SHARED.upcallStub(
                MH_RUN_testMultipleCbArgs_f,
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS),
                Arena.global());
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static final MethodHandle MH_RUN_testMultipleCbArgs_g;
    private static final MemorySegment UPCALL_testMultipleCbArgs_g;
    static {
        try {
            MH_RUN_testMultipleCbArgs_g = MethodHandles.lookup().findStatic(
                CallbackWrapper.class, "runCallback_testMultipleCbArgs_g",
                MethodType.methodType(int.class, MemorySegment.class, int.class));
            UPCALL_testMultipleCbArgs_g = DiplomatLib.LINKER_SHARED.upcallStub(
                MH_RUN_testMultipleCbArgs_g,
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
                Arena.global());
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static final MethodHandle MH_RUN_testStrCbArg_f;
    private static final MemorySegment UPCALL_testStrCbArg_f;
    static {
        try {
            MH_RUN_testStrCbArg_f = MethodHandles.lookup().findStatic(
                CallbackWrapper.class, "runCallback_testStrCbArg_f",
                MethodType.methodType(int.class, MemorySegment.class, MemorySegment.class));
            UPCALL_testStrCbArg_f = DiplomatLib.LINKER_SHARED.upcallStub(
                MH_RUN_testStrCbArg_f,
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW),
                Arena.global());
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static final MethodHandle MH_RUN_testSliceCbArg_f;
    private static final MemorySegment UPCALL_testSliceCbArg_f;
    static {
        try {
            MH_RUN_testSliceCbArg_f = MethodHandles.lookup().findStatic(
                CallbackWrapper.class, "runCallback_testSliceCbArg_f",
                MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class));
            UPCALL_testSliceCbArg_f = DiplomatLib.LINKER_SHARED.upcallStub(
                MH_RUN_testSliceCbArg_f,
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW),
                Arena.global());
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public boolean cantBeEmpty;

    public CallbackWrapper() {
    }

    CallbackWrapper(boolean cantBeEmpty) {
        this.cantBeEmpty = cantBeEmpty;
    }

    static CallbackWrapper fromNative(MemorySegment seg) {
        return new CallbackWrapper(
            (boolean) VH_CANT_BE_EMPTY.get(seg, 0L)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_CANT_BE_EMPTY.set(seg, 0L, this.cantBeEmpty);
        return seg;
    }

    public static int testMultiArgCallback(TestMultiArgCallbackF f, int x) {
        try (var arena = Arena.ofConfined()) {
            long fId = DiplomatLib.registerCallback(f);
            var fNative = arena.allocate(DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT);
            DiplomatLib.VH_CB_DATA.set(fNative, 0L, MemorySegment.ofAddress(fId));
            DiplomatLib.VH_CB_RUN.set(fNative, 0L, UPCALL_testMultiArgCallback_f);
            DiplomatLib.VH_CB_DESTRUCTOR.set(fNative, 0L, DiplomatLib.DESTRUCTOR_STUB);
            return (int) CALLBACKWRAPPER_TEST_MULTI_ARG_CALLBACK.invokeExact(fNative, x);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int testNoArgs(TestNoArgsH h) {
        try (var arena = Arena.ofConfined()) {
            long hId = DiplomatLib.registerCallback(h);
            var hNative = arena.allocate(DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT);
            DiplomatLib.VH_CB_DATA.set(hNative, 0L, MemorySegment.ofAddress(hId));
            DiplomatLib.VH_CB_RUN.set(hNative, 0L, UPCALL_testNoArgs_h);
            DiplomatLib.VH_CB_DESTRUCTOR.set(hNative, 0L, DiplomatLib.DESTRUCTOR_STUB);
            return (int) CALLBACKWRAPPER_TEST_NO_ARGS.invokeExact(hNative);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int testCbWithStruct(TestCbWithStructF f) {
        try (var arena = Arena.ofConfined()) {
            long fId = DiplomatLib.registerCallback(f);
            var fNative = arena.allocate(DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT);
            DiplomatLib.VH_CB_DATA.set(fNative, 0L, MemorySegment.ofAddress(fId));
            DiplomatLib.VH_CB_RUN.set(fNative, 0L, UPCALL_testCbWithStruct_f);
            DiplomatLib.VH_CB_DESTRUCTOR.set(fNative, 0L, DiplomatLib.DESTRUCTOR_STUB);
            return (int) CALLBACKWRAPPER_TEST_CB_WITH_STRUCT.invokeExact(fNative);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int testMultipleCbArgs(TestMultipleCbArgsF f, TestMultipleCbArgsG g) {
        try (var arena = Arena.ofConfined()) {
            long fId = DiplomatLib.registerCallback(f);
            var fNative = arena.allocate(DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT);
            DiplomatLib.VH_CB_DATA.set(fNative, 0L, MemorySegment.ofAddress(fId));
            DiplomatLib.VH_CB_RUN.set(fNative, 0L, UPCALL_testMultipleCbArgs_f);
            DiplomatLib.VH_CB_DESTRUCTOR.set(fNative, 0L, DiplomatLib.DESTRUCTOR_STUB);
            long gId = DiplomatLib.registerCallback(g);
            var gNative = arena.allocate(DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT);
            DiplomatLib.VH_CB_DATA.set(gNative, 0L, MemorySegment.ofAddress(gId));
            DiplomatLib.VH_CB_RUN.set(gNative, 0L, UPCALL_testMultipleCbArgs_g);
            DiplomatLib.VH_CB_DESTRUCTOR.set(gNative, 0L, DiplomatLib.DESTRUCTOR_STUB);
            return (int) CALLBACKWRAPPER_TEST_MULTIPLE_CB_ARGS.invokeExact(fNative, gNative);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int testStrCbArg(TestStrCbArgF f) {
        try (var arena = Arena.ofConfined()) {
            long fId = DiplomatLib.registerCallback(f);
            var fNative = arena.allocate(DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT);
            DiplomatLib.VH_CB_DATA.set(fNative, 0L, MemorySegment.ofAddress(fId));
            DiplomatLib.VH_CB_RUN.set(fNative, 0L, UPCALL_testStrCbArg_f);
            DiplomatLib.VH_CB_DESTRUCTOR.set(fNative, 0L, DiplomatLib.DESTRUCTOR_STUB);
            return (int) CALLBACKWRAPPER_TEST_STR_CB_ARG.invokeExact(fNative);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void testSliceCbArg(byte[] arg, TestSliceCbArgF f) {
        try (var arena = Arena.ofConfined()) {
            var argSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, arg);
            long fId = DiplomatLib.registerCallback(f);
            var fNative = arena.allocate(DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT);
            DiplomatLib.VH_CB_DATA.set(fNative, 0L, MemorySegment.ofAddress(fId));
            DiplomatLib.VH_CB_RUN.set(fNative, 0L, UPCALL_testSliceCbArg_f);
            DiplomatLib.VH_CB_DESTRUCTOR.set(fNative, 0L, DiplomatLib.DESTRUCTOR_STUB);
            CALLBACKWRAPPER_TEST_SLICE_CB_ARG.invokeExact(argSeg, (long) arg.length, fNative);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}