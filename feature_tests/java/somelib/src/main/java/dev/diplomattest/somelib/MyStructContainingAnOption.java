package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class MyStructContainingAnOption {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.structLayout(MyStruct.LAYOUT.withName("value"), ValueLayout.JAVA_BOOLEAN.withName("is_ok"), MemoryLayout.paddingLayout(7)).withName("a"),
        MemoryLayout.structLayout(ValueLayout.JAVA_INT.withName("value"), ValueLayout.JAVA_BOOLEAN.withName("is_ok"), MemoryLayout.paddingLayout(3)).withName("b")
    );
    private static final VarHandle VH_A_IS_OK = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"), MemoryLayout.PathElement.groupElement("is_ok"));
    private static final VarHandle VH_B_VALUE = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("b"), MemoryLayout.PathElement.groupElement("value"));
    private static final VarHandle VH_B_IS_OK = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("b"), MemoryLayout.PathElement.groupElement("is_ok"));
    private static final long OFFSET_A = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("a"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle MYSTRUCTCONTAININGANOPTION_NEW;
    private static final MethodHandle MYSTRUCTCONTAININGANOPTION_FILLED;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        MYSTRUCTCONTAININGANOPTION_NEW = LINKER.downcallHandle(
            LIB.find("MyStructContainingAnOption_new").orElseThrow(),
            FunctionDescriptor.of(MyStructContainingAnOption.LAYOUT)
        );
        MYSTRUCTCONTAININGANOPTION_FILLED = LINKER.downcallHandle(
            LIB.find("MyStructContainingAnOption_filled").orElseThrow(),
            FunctionDescriptor.of(MyStructContainingAnOption.LAYOUT)
        );
    }

    public MyStruct a;

    public DefaultEnum b;

    private MyStructContainingAnOption(Void _internal) {
    }

    public MyStructContainingAnOption() {
        try (var arena = Arena.ofConfined()) {
            var seg = (MemorySegment) MYSTRUCTCONTAININGANOPTION_NEW.invokeExact((SegmentAllocator) arena);
            this.a = (boolean) VH_A_IS_OK.get(seg, 0L) ? MyStruct.fromNative(seg.asSlice(OFFSET_A, MyStruct.LAYOUT.byteSize())) : null;
            this.b = (boolean) VH_B_IS_OK.get(seg, 0L) ? DefaultEnum.fromNative((int) VH_B_VALUE.get(seg, 0L)) : null;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static MyStructContainingAnOption fromNative(MemorySegment seg) {
        var result = new MyStructContainingAnOption((Void) null);
        result.a = (boolean) VH_A_IS_OK.get(seg, 0L) ? MyStruct.fromNative(seg.asSlice(OFFSET_A, MyStruct.LAYOUT.byteSize())) : null;
        result.b = (boolean) VH_B_IS_OK.get(seg, 0L) ? DefaultEnum.fromNative((int) VH_B_VALUE.get(seg, 0L)) : null;
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        if (this.a != null) { seg.asSlice(OFFSET_A, MyStruct.LAYOUT.byteSize()).copyFrom(this.a.toNative(arena)); VH_A_IS_OK.set(seg, 0L, true); } else { VH_A_IS_OK.set(seg, 0L, false); }
        if (this.b != null) { VH_B_VALUE.set(seg, 0L, this.b.toNative()); VH_B_IS_OK.set(seg, 0L, true); } else { VH_B_IS_OK.set(seg, 0L, false); }
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.a = (boolean) VH_A_IS_OK.get(seg, 0L) ? MyStruct.fromNative(seg.asSlice(OFFSET_A, MyStruct.LAYOUT.byteSize())) : null;
        this.b = (boolean) VH_B_IS_OK.get(seg, 0L) ? DefaultEnum.fromNative((int) VH_B_VALUE.get(seg, 0L)) : null;
    }

    public static MyStructContainingAnOption filled() {
        try (var arena = Arena.ofConfined()) {
            return MyStructContainingAnOption.fromNative((MemorySegment) MYSTRUCTCONTAININGANOPTION_FILLED.invokeExact((SegmentAllocator) arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}