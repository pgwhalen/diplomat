package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class MyStructContainingAnOption {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("a"),
        ValueLayout.JAVA_BYTE.withName("b")
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

    public Object a;

    public Object b;

    public MyStructContainingAnOption() {
    }

    MyStructContainingAnOption(Object a, Object b) {
        this.a = a;
        this.b = b;
    }

    static MyStructContainingAnOption fromNative(MemorySegment seg) {
        var result = new MyStructContainingAnOption();
        result.a = null /* unsupported field a */;
        result.b = null /* unsupported field b */;
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        // unsupported field a
        // unsupported field b
        return seg;
    }

    public static MyStructContainingAnOption new_() {
        try (var arena = Arena.ofConfined()) {
            return MyStructContainingAnOption.fromNative((MemorySegment) MYSTRUCTCONTAININGANOPTION_NEW.invokeExact((SegmentAllocator) arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
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