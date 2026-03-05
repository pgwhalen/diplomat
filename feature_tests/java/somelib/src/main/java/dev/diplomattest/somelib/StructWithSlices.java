package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class StructWithSlices {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("first"),
        DiplomatLib.DIPLOMAT_STRING_VIEW.withName("second")
    );
    private static final VarHandle VH_FIRST_DATA = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("first"), MemoryLayout.PathElement.groupElement("data"));
    private static final VarHandle VH_FIRST_LEN = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("first"), MemoryLayout.PathElement.groupElement("len"));
    private static final VarHandle VH_SECOND_DATA = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("second"), MemoryLayout.PathElement.groupElement("data"));
    private static final VarHandle VH_SECOND_LEN = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("second"), MemoryLayout.PathElement.groupElement("len"));

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
        return new StructWithSlices(
            new String(((MemorySegment) VH_FIRST_DATA.get(seg, 0L)).reinterpret((long) VH_FIRST_LEN.get(seg, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8),
            ((MemorySegment) VH_SECOND_DATA.get(seg, 0L)).reinterpret((long) VH_SECOND_LEN.get(seg, 0L) * 2L).toArray(ValueLayout.JAVA_SHORT)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        { byte[] firstBytes = this.first.getBytes(StandardCharsets.UTF_8); var firstSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, firstBytes); VH_FIRST_DATA.set(seg, 0L, firstSeg); VH_FIRST_LEN.set(seg, 0L, (long) firstBytes.length); }
        { var secondSeg = arena.allocateFrom(ValueLayout.JAVA_SHORT, this.second); VH_SECOND_DATA.set(seg, 0L, secondSeg); VH_SECOND_LEN.set(seg, 0L, (long) this.second.length); }
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.first = new String(((MemorySegment) VH_FIRST_DATA.get(seg, 0L)).reinterpret((long) VH_FIRST_LEN.get(seg, 0L)).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8);
        this.second = ((MemorySegment) VH_SECOND_DATA.get(seg, 0L)).reinterpret((long) VH_SECOND_LEN.get(seg, 0L) * 2L).toArray(ValueLayout.JAVA_SHORT);
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