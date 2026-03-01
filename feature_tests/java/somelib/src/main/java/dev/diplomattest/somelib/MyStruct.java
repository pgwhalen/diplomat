package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class MyStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("a"),
        ValueLayout.JAVA_BOOLEAN.withName("b"),
        ValueLayout.JAVA_BYTE.withName("c"),
        MemoryLayout.paddingLayout(5),
        ValueLayout.JAVA_LONG.withName("d"),
        ValueLayout.JAVA_INT.withName("e"),
        ValueLayout.JAVA_INT.withName("f"),
        ValueLayout.JAVA_INT.withName("g"),
        MemoryLayout.paddingLayout(4)
    );

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle MYSTRUCT_NEW;
    private static final MethodHandle MYSTRUCT_INTO_A;
    private static final MethodHandle MYSTRUCT_RETURNS_ZST_RESULT;
    private static final MethodHandle MYSTRUCT_FAILS_ZST_RESULT;
    static final StructLayout MYSTRUCT_RETURNS_ZST_RESULT_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_BOOLEAN.withName("is_ok")
        );
    static final StructLayout MYSTRUCT_FAILS_ZST_RESULT_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_BOOLEAN.withName("is_ok")
        );

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        MYSTRUCT_NEW = LINKER.downcallHandle(
            LIB.find("MyStruct_new").orElseThrow(),
            FunctionDescriptor.of(MyStruct.LAYOUT)
        );
        MYSTRUCT_INTO_A = LINKER.downcallHandle(
            LIB.find("MyStruct_into_a").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_BYTE, MyStruct.LAYOUT)
        );
        MYSTRUCT_RETURNS_ZST_RESULT = LINKER.downcallHandle(
            LIB.find("MyStruct_returns_zst_result").orElseThrow(),
            FunctionDescriptor.of(MYSTRUCT_RETURNS_ZST_RESULT_RESULT)
        );
        MYSTRUCT_FAILS_ZST_RESULT = LINKER.downcallHandle(
            LIB.find("MyStruct_fails_zst_result").orElseThrow(),
            FunctionDescriptor.of(MYSTRUCT_FAILS_ZST_RESULT_RESULT)
        );
    }

    public byte a;

    public boolean b;

    public byte c;

    public long d;

    public int e;

    public int f;

    public MyEnum g;

    private MyStruct(Void _internal) {
    }

    public MyStruct() {
        try (var arena = Arena.ofConfined()) {
            var seg = (MemorySegment) MYSTRUCT_NEW.invokeExact((SegmentAllocator) arena);
            this.a = (byte) seg.get(ValueLayout.JAVA_BYTE, 0L);
            this.b = (boolean) seg.get(ValueLayout.JAVA_BOOLEAN, 1L);
            this.c = (byte) seg.get(ValueLayout.JAVA_BYTE, 2L);
            this.d = (long) seg.get(ValueLayout.JAVA_LONG, 8L);
            this.e = (int) seg.get(ValueLayout.JAVA_INT, 16L);
            this.f = (int) seg.get(ValueLayout.JAVA_INT, 20L);
            this.g = MyEnum.fromNative((int) seg.get(ValueLayout.JAVA_INT, 24L));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static MyStruct fromNative(MemorySegment seg) {
        var result = new MyStruct((Void) null);
        result.a = (byte) seg.get(ValueLayout.JAVA_BYTE, 0L);
        result.b = (boolean) seg.get(ValueLayout.JAVA_BOOLEAN, 1L);
        result.c = (byte) seg.get(ValueLayout.JAVA_BYTE, 2L);
        result.d = (long) seg.get(ValueLayout.JAVA_LONG, 8L);
        result.e = (int) seg.get(ValueLayout.JAVA_INT, 16L);
        result.f = (int) seg.get(ValueLayout.JAVA_INT, 20L);
        result.g = MyEnum.fromNative((int) seg.get(ValueLayout.JAVA_INT, 24L));
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        seg.set(ValueLayout.JAVA_BYTE, 0L, this.a);
        seg.set(ValueLayout.JAVA_BOOLEAN, 1L, this.b);
        seg.set(ValueLayout.JAVA_BYTE, 2L, this.c);
        seg.set(ValueLayout.JAVA_LONG, 8L, this.d);
        seg.set(ValueLayout.JAVA_INT, 16L, this.e);
        seg.set(ValueLayout.JAVA_INT, 20L, this.f);
        seg.set(ValueLayout.JAVA_INT, 24L, this.g.toNative());
        return seg;
    }

    public static void returnsZstResult() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) MYSTRUCT_RETURNS_ZST_RESULT.invokeExact((SegmentAllocator) arena);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 0L);
            if (isOk) {
                return;
            } else {
                throw MyZst.fromNative(result.asSlice(0L, MyZst.LAYOUT.byteSize()));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void failsZstResult() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) MYSTRUCT_FAILS_ZST_RESULT.invokeExact((SegmentAllocator) arena);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 0L);
            if (isOk) {
                return;
            } else {
                throw MyZst.fromNative(result.asSlice(0L, MyZst.LAYOUT.byteSize()));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public byte intoA() {
        try (var arena = Arena.ofConfined()) {
            return (byte) MYSTRUCT_INTO_A.invokeExact(this.toNative(arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}