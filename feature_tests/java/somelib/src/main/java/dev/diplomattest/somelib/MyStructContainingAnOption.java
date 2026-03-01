package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class MyStructContainingAnOption {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.structLayout(MyStruct.LAYOUT, ValueLayout.JAVA_BOOLEAN, MemoryLayout.paddingLayout(7)).withName("a"),
        MemoryLayout.structLayout(ValueLayout.JAVA_INT, ValueLayout.JAVA_BOOLEAN, MemoryLayout.paddingLayout(3)).withName("b")
    );

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
            this.a = seg.get(ValueLayout.JAVA_BOOLEAN, 32L) ? MyStruct.fromNative(seg.asSlice(0L, MyStruct.LAYOUT.byteSize())) : null;
            this.b = seg.get(ValueLayout.JAVA_BOOLEAN, 44L) ? DefaultEnum.fromNative((int) seg.get(ValueLayout.JAVA_INT, 40L)) : null;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static MyStructContainingAnOption fromNative(MemorySegment seg) {
        var result = new MyStructContainingAnOption((Void) null);
        result.a = seg.get(ValueLayout.JAVA_BOOLEAN, 32L) ? MyStruct.fromNative(seg.asSlice(0L, MyStruct.LAYOUT.byteSize())) : null;
        result.b = seg.get(ValueLayout.JAVA_BOOLEAN, 44L) ? DefaultEnum.fromNative((int) seg.get(ValueLayout.JAVA_INT, 40L)) : null;
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        if (this.a != null) { seg.asSlice(0L, MyStruct.LAYOUT.byteSize()).copyFrom(this.a.toNative(arena)); seg.set(ValueLayout.JAVA_BOOLEAN, 32L, true); } else { seg.set(ValueLayout.JAVA_BOOLEAN, 32L, false); }
        if (this.b != null) { seg.set(ValueLayout.JAVA_INT, 40L, this.b.toNative()); seg.set(ValueLayout.JAVA_BOOLEAN, 44L, true); } else { seg.set(ValueLayout.JAVA_BOOLEAN, 44L, false); }
        return seg;
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