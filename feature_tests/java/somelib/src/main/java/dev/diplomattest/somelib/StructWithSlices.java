package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class StructWithSlices {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("first"),
        ValueLayout.JAVA_BYTE.withName("second")
    );

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle STRUCTWITHSLICES_RETURN_LAST;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        STRUCTWITHSLICES_RETURN_LAST = LINKER.downcallHandle(
            LIB.find("StructWithSlices_return_last").orElseThrow(),
            FunctionDescriptor.ofVoid(StructWithSlices.LAYOUT, ValueLayout.ADDRESS)
        );
    }

    public Object first;

    public Object second;

    public StructWithSlices() {
    }

    StructWithSlices(Object first, Object second) {
        this.first = first;
        this.second = second;
    }

    static StructWithSlices fromNative(MemorySegment seg) {
        var result = new StructWithSlices();
        result.first = null /* unsupported field first */;
        result.second = null /* unsupported field second */;
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        // unsupported field first
        // unsupported field second
        return seg;
    }

    public String returnLast() {
        var write = DiplomatLib.createWrite();
        try (var arena = Arena.ofConfined()) {
            STRUCTWITHSLICES_RETURN_LAST.invokeExact(this.toNative(arena), write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}