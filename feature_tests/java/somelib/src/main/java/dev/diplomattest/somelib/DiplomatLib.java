package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Diplomat runtime support. */
public final class DiplomatLib {
    private DiplomatLib() {}

    static final StructLayout DIPLOMAT_STRING_VIEW = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("data"),
        ValueLayout.JAVA_LONG.withName("len")
    );
    static final java.lang.invoke.VarHandle VH_SV_DATA =
        DIPLOMAT_STRING_VIEW.varHandle(MemoryLayout.PathElement.groupElement("data"));
    static final java.lang.invoke.VarHandle VH_SV_LEN =
        DIPLOMAT_STRING_VIEW.varHandle(MemoryLayout.PathElement.groupElement("len"));

    // Callback support: DiplomatCallback = { ADDRESS data, ADDRESS run_callback, ADDRESS destructor }
    static final StructLayout DIPLOMAT_CALLBACK_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("data"),
        ValueLayout.ADDRESS.withName("run_callback"),
        ValueLayout.ADDRESS.withName("destructor")
    );
    static final java.lang.invoke.VarHandle VH_CB_DATA =
        DIPLOMAT_CALLBACK_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("data"));
    static final java.lang.invoke.VarHandle VH_CB_RUN =
        DIPLOMAT_CALLBACK_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("run_callback"));
    static final java.lang.invoke.VarHandle VH_CB_DESTRUCTOR =
        DIPLOMAT_CALLBACK_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("destructor"));

    // Prevent GC: maps callback IDs to Java objects so they stay alive
    static final ConcurrentHashMap<Long, Object> PREVENT_GC = new ConcurrentHashMap<>();
    private static final AtomicLong NEXT_CALLBACK_ID = new AtomicLong(1);

    static long registerCallback(Object callback) {
        long id = NEXT_CALLBACK_ID.getAndIncrement();
        PREVENT_GC.put(id, callback);
        return id;
    }

    static void unregisterCallback(long id) {
        PREVENT_GC.remove(id);
    }

    @SuppressWarnings("unchecked")
    static <T> T getCallback(long id, Class<T> clazz) {
        return clazz.cast(PREVENT_GC.get(id));
    }

    // Called by Rust destructor to release the Java callback reference
    static void callbackDestructor(MemorySegment data) {
        long id = data.address();
        unregisterCallback(id);
    }

    static final Linker LINKER_SHARED = Linker.nativeLinker();
    private static final Linker LINKER = LINKER_SHARED;
    private static final SymbolLookup LIB;

    private static final MethodHandle DIPLOMAT_BUFFER_WRITE_CREATE;
    private static final MethodHandle DIPLOMAT_BUFFER_WRITE_GET_BYTES;
    private static final MethodHandle DIPLOMAT_BUFFER_WRITE_LEN;
    private static final MethodHandle DIPLOMAT_BUFFER_WRITE_DESTROY;

    // Shared destructor upcall stub (calls callbackDestructor)
    static final MemorySegment DESTRUCTOR_STUB;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DIPLOMAT_BUFFER_WRITE_CREATE = LINKER.downcallHandle(
            LIB.find("diplomat_buffer_write_create").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        DIPLOMAT_BUFFER_WRITE_GET_BYTES = LINKER.downcallHandle(
            LIB.find("diplomat_buffer_write_get_bytes").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        DIPLOMAT_BUFFER_WRITE_LEN = LINKER.downcallHandle(
            LIB.find("diplomat_buffer_write_len").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
        );
        DIPLOMAT_BUFFER_WRITE_DESTROY = LINKER.downcallHandle(
            LIB.find("diplomat_buffer_write_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        try {
            MethodHandle destructorMH = MethodHandles.lookup().findStatic(
                DiplomatLib.class, "callbackDestructor",
                MethodType.methodType(void.class, MemorySegment.class));
            DESTRUCTOR_STUB = LINKER_SHARED.upcallStub(
                destructorMH,
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                Arena.global());
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static MemorySegment createWrite() {
        try {
            return (MemorySegment) DIPLOMAT_BUFFER_WRITE_CREATE.invokeExact(0L);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static void destroyWrite(MemorySegment write) {
        try {
            DIPLOMAT_BUFFER_WRITE_DESTROY.invokeExact(write);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static String writeToString(MemorySegment write) {
        try {
            var bytes = (MemorySegment) DIPLOMAT_BUFFER_WRITE_GET_BYTES.invokeExact(write);
            long len = (long) DIPLOMAT_BUFFER_WRITE_LEN.invokeExact(write);
            byte[] byteArray = bytes.reinterpret(len).toArray(ValueLayout.JAVA_BYTE);
            return new String(byteArray, StandardCharsets.UTF_8);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                DIPLOMAT_BUFFER_WRITE_DESTROY.invokeExact(write);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}