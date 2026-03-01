package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class StructWithSlices {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("first"),
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("second")
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

    public String first;

    public short[] second;

    public StructWithSlices() {
    }

    StructWithSlices(String first, short[] second) {
        this.first = first;
        this.second = second;
    }

    static StructWithSlices fromNative(MemorySegment seg) {
        var result = new StructWithSlices();
        result.first = new String(seg.get(ValueLayout.ADDRESS, 0L).reinterpret(seg.get(ValueLayout.JAVA_LONG, 8L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
        result.second = seg.get(ValueLayout.ADDRESS, 16L).reinterpret(seg.get(ValueLayout.JAVA_LONG, 24L) * 2L).toArray(ValueLayout.JAVA_SHORT);
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        { byte[] firstBytes = this.first.getBytes(StandardCharsets.UTF_8); var firstSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, firstBytes); seg.set(ValueLayout.ADDRESS, 0L, firstSeg); seg.set(ValueLayout.JAVA_LONG, 8L, (long) firstBytes.length); }
        { var secondSeg = arena.allocateFrom(ValueLayout.JAVA_SHORT, this.second); seg.set(ValueLayout.ADDRESS, 16L, secondSeg); seg.set(ValueLayout.JAVA_LONG, 24L, (long) this.second.length); }
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