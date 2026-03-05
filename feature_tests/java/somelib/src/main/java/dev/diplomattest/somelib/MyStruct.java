package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
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
    private static final VarHandle VH_A = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"));
    private static final VarHandle VH_B = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("b"));
    private static final VarHandle VH_C = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("c"));
    private static final VarHandle VH_D = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("d"));
    private static final VarHandle VH_E = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("e"));
    private static final VarHandle VH_F = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("f"));
    private static final VarHandle VH_G = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("g"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle MYSTRUCT_NEW;
    private static final MethodHandle MYSTRUCT_TAKES_MUT;
    private static final MethodHandle MYSTRUCT_TAKES_CONST;
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
        MYSTRUCT_TAKES_MUT = LINKER.downcallHandle(
            LIB.find("MyStruct_takes_mut").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        MYSTRUCT_TAKES_CONST = LINKER.downcallHandle(
            LIB.find("MyStruct_takes_const").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
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
            this.a = (byte) VH_A.get(seg, 0L);
            this.b = (boolean) VH_B.get(seg, 0L);
            this.c = (byte) VH_C.get(seg, 0L);
            this.d = (long) VH_D.get(seg, 0L);
            this.e = (int) VH_E.get(seg, 0L);
            this.f = (int) VH_F.get(seg, 0L);
            this.g = MyEnum.fromNative((int) VH_G.get(seg, 0L));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static MyStruct fromNative(MemorySegment seg) {
        var result = new MyStruct((Void) null);
        result.a = (byte) VH_A.get(seg, 0L);
        result.b = (boolean) VH_B.get(seg, 0L);
        result.c = (byte) VH_C.get(seg, 0L);
        result.d = (long) VH_D.get(seg, 0L);
        result.e = (int) VH_E.get(seg, 0L);
        result.f = (int) VH_F.get(seg, 0L);
        result.g = MyEnum.fromNative((int) VH_G.get(seg, 0L));
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_A.set(seg, 0L, this.a);
        VH_B.set(seg, 0L, this.b);
        VH_C.set(seg, 0L, this.c);
        VH_D.set(seg, 0L, this.d);
        VH_E.set(seg, 0L, this.e);
        VH_F.set(seg, 0L, this.f);
        VH_G.set(seg, 0L, this.g.toNative());
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.a = (byte) VH_A.get(seg, 0L);
        this.b = (boolean) VH_B.get(seg, 0L);
        this.c = (byte) VH_C.get(seg, 0L);
        this.d = (long) VH_D.get(seg, 0L);
        this.e = (int) VH_E.get(seg, 0L);
        this.f = (int) VH_F.get(seg, 0L);
        this.g = MyEnum.fromNative((int) VH_G.get(seg, 0L));
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

    public void takesMut(MyStruct o) {
        try (var arena = Arena.ofConfined()) {
            var selfSeg = this.toNative(arena);
            var oSeg = o.toNative(arena);
            MYSTRUCT_TAKES_MUT.invokeExact(selfSeg, oSeg);
            this.updateFromNative(selfSeg);
            o.updateFromNative(oSeg);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void takesConst(MyStruct o) {
        try (var arena = Arena.ofConfined()) {
            var selfSeg = this.toNative(arena);
            var oSeg = o.toNative(arena);
            MYSTRUCT_TAKES_CONST.invokeExact(selfSeg, oSeg);
            o.updateFromNative(oSeg);
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