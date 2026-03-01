package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RenamedMyIterator implements AutoCloseable, Iterator<Byte> {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_MYITERATOR_NEXT;
    static final StructLayout NAMESPACE_MYITERATOR_NEXT_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_BYTE.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok")
        );

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("namespace_MyIterator_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_MYITERATOR_NEXT = LINKER.downcallHandle(
            LIB.find("namespace_MyIterator_next").orElseThrow(),
            FunctionDescriptor.of(NAMESPACE_MYITERATOR_NEXT_RESULT, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;
    private Byte nextVal;

    RenamedMyIterator(MemorySegment handle) {
        this.handle = handle;
        this.nextVal = nextInternal();
    }

    @Override
    public void close() {
        try {
            DESTROY.invokeExact(handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private Byte nextInternal() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) NAMESPACE_MYITERATOR_NEXT.invokeExact((SegmentAllocator) arena, handle);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 1L);
            if (isOk) {
                return (byte) result.get(ValueLayout.JAVA_BYTE, 0L);
            } else {
                return null;
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean hasNext() {
        return nextVal != null;
    }

    @Override
    public Byte next() {
        Byte returnVal = nextVal;
        if (returnVal == null) {
            throw new NoSuchElementException();
        }
        nextVal = nextInternal();
        return returnVal;
    }
}