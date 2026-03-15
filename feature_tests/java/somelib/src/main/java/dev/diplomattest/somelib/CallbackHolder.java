package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class CallbackHolder implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle CALLBACKHOLDER_NEW;
    private static final MethodHandle CALLBACKHOLDER_CALL;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("CallbackHolder_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        CALLBACKHOLDER_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("CallbackHolder_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT)
        );
        CALLBACKHOLDER_CALL = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("CallbackHolder_call").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
    }

    @FunctionalInterface
    public interface NewFunc {
        int invoke(int arg0);
    }

    private static int runCallback_new__func(MemorySegment data, int arg0) {
        @SuppressWarnings("unchecked")
        NewFunc cb = DiplomatLib.getCallback(data, NewFunc.class);
        return cb.invoke(arg0);
    }

    private static final MethodHandle MH_RUN_new__func;
    private static final MemorySegment UPCALL_new__func;
    static {
        try {
            MH_RUN_new__func = MethodHandles.lookup().findStatic(
                CallbackHolder.class, "runCallback_new__func",
                MethodType.methodType(int.class, MemorySegment.class, int.class));
            UPCALL_new__func = DiplomatLib.LINKER_SHARED.upcallStub(
                MH_RUN_new__func,
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
                Arena.global());
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    final MemorySegment handle;

    CallbackHolder(MemorySegment handle) {
        this.handle = handle;
    }

    public CallbackHolder(NewFunc func) {
        try (var arena = Arena.ofConfined()) {
            var funcId = DiplomatLib.registerCallback(func);
            var funcNative = arena.allocate(DiplomatLib.DIPLOMAT_CALLBACK_LAYOUT);
            DiplomatLib.VH_CB_DATA.set(funcNative, 0L, funcId);
            DiplomatLib.VH_CB_RUN.set(funcNative, 0L, UPCALL_new__func);
            DiplomatLib.VH_CB_DESTRUCTOR.set(funcNative, 0L, DiplomatLib.DESTRUCTOR_STUB);
            this.handle = (MemorySegment) CALLBACKHOLDER_NEW.invokeExact(funcNative);
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

    public int call(int a) {
        try {
            return (int) CALLBACKHOLDER_CALL.invokeExact(handle, a);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}