package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class MyString implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle MYSTRING_NEW;
    private static final MethodHandle MYSTRING_NEW_UNSAFE;
    private static final MethodHandle MYSTRING_SET_STR;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("MyString_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        MYSTRING_NEW = LINKER.downcallHandle(
            LIB.find("MyString_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        MYSTRING_NEW_UNSAFE = LINKER.downcallHandle(
            LIB.find("MyString_new_unsafe").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
        MYSTRING_SET_STR = LINKER.downcallHandle(
            LIB.find("MyString_set_str").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        );
    }

    final MemorySegment handle;

    MyString(MemorySegment handle) {
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

    public static MyString new_(String v) {
        try (var arena = Arena.ofConfined()) {
            byte[] vBytes = v.getBytes(StandardCharsets.UTF_8);

            var vSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, vBytes);
            return new MyString((MemorySegment) MYSTRING_NEW.invokeExact(vSeg, (long) vBytes.length));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static MyString newUnsafe(String v) {
        try (var arena = Arena.ofConfined()) {
            byte[] vBytes = v.getBytes(StandardCharsets.UTF_8);

            var vSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, vBytes);
            return new MyString((MemorySegment) MYSTRING_NEW_UNSAFE.invokeExact(vSeg, (long) vBytes.length));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setStr(String newStr) {
        try (var arena = Arena.ofConfined()) {
            byte[] newStrBytes = newStr.getBytes(StandardCharsets.UTF_8);

            var newStrSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, newStrBytes);
            MYSTRING_SET_STR.invokeExact(handle, newStrSeg, (long) newStrBytes.length);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}