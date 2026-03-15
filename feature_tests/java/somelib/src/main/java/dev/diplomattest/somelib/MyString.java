package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class MyString implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle MYSTRING_NEW;
    private static final MethodHandle MYSTRING_NEW_UNSAFE;
    private static final MethodHandle MYSTRING_NEW_OWNED;
    private static final MethodHandle MYSTRING_NEW_FROM_FIRST;
    private static final MethodHandle MYSTRING_SET_STR;
    private static final MethodHandle MYSTRING_GET_STR;
    private static final MethodHandle MYSTRING_STRING_TRANSFORM;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MyString_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        MYSTRING_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MyString_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        MYSTRING_NEW_UNSAFE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MyString_new_unsafe").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        MYSTRING_NEW_OWNED = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MyString_new_owned").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        MYSTRING_NEW_FROM_FIRST = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MyString_new_from_first").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        MYSTRING_SET_STR = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MyString_set_str").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        MYSTRING_GET_STR = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MyString_get_str").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        MYSTRING_STRING_TRANSFORM = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("MyString_string_transform").orElseThrow(),
            FunctionDescriptor.ofVoid(DiplomatLib.DIPLOMAT_STRING_VIEW, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    MyString(MemorySegment handle) {
        this.handle = handle;
    }

    public MyString(String v) {
        try (var arena = Arena.ofConfined()) {
            byte[] vBytes = v.getBytes(StandardCharsets.UTF_8);

            var vSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, vBytes);
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) vBytes.length);
            this.handle = (MemorySegment) MYSTRING_NEW.invokeExact(vSlice);
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

    public static MyString unsafe(String v) {
        try (var arena = Arena.ofConfined()) {
            byte[] vBytes = v.getBytes(StandardCharsets.UTF_8);

            var vSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, vBytes);
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) vBytes.length);
            return new MyString((MemorySegment) MYSTRING_NEW_UNSAFE.invokeExact(vSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static MyString newOwned(String v) {
        try (var arena = Arena.ofConfined()) {
            byte[] vBytes = v.getBytes(StandardCharsets.UTF_8);
            var vSrc = MemorySegment.ofArray(vBytes);
            var vSeg = DiplomatLib.diplomatAlloc((long) vBytes.length, 1L).reinterpret((long) vBytes.length);
            MemorySegment.copy(vSrc, 0, vSeg, 0, (long) vBytes.length);
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) vBytes.length);
            return new MyString((MemorySegment) MYSTRING_NEW_OWNED.invokeExact(vSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static MyString newFromFirst(String[] v) {
        try (var arena = Arena.ofConfined()) {
            MemorySegment vSeg = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW, v.length);
            for (int i = 0; i < v.length; i++) {
                byte[] vBytes_i = v[i].getBytes(StandardCharsets.UTF_8);
                var vData_i = arena.allocateFrom(ValueLayout.JAVA_BYTE, vBytes_i);
                long vOff = i * 16L;
                vSeg.set(ValueLayout.ADDRESS, vOff, vData_i);
                vSeg.set(ValueLayout.JAVA_LONG, vOff + 8L, (long) vBytes_i.length);
            }
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) v.length);
            return new MyString((MemorySegment) MYSTRING_NEW_FROM_FIRST.invokeExact(vSlice));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String stringTransform(String foo) {
        var write = DiplomatLib.createWrite();
        try (var arena = Arena.ofConfined()) {
            byte[] fooBytes = foo.getBytes(StandardCharsets.UTF_8);

            var fooSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, fooBytes);
            var fooSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(fooSlice, 0L, fooSeg);
            DiplomatLib.VH_SV_LEN.set(fooSlice, 0L, (long) fooBytes.length);
            MYSTRING_STRING_TRANSFORM.invokeExact(fooSlice, write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setStr(String newStr) {
        try (var arena = Arena.ofConfined()) {
            byte[] newStrBytes = newStr.getBytes(StandardCharsets.UTF_8);

            var newStrSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, newStrBytes);
            var newStrSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(newStrSlice, 0L, newStrSeg);
            DiplomatLib.VH_SV_LEN.set(newStrSlice, 0L, (long) newStrBytes.length);
            MYSTRING_SET_STR.invokeExact(handle, newStrSlice);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getStr() {
        var write = DiplomatLib.createWrite();
        try {
            MYSTRING_GET_STR.invokeExact(handle, write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}