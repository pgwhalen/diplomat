package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class StructOfOpaque {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("i"),
        ValueLayout.ADDRESS.withName("j")
    );
    private static final VarHandle VH_I = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("i"));
    private static final VarHandle VH_J = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("j"));
    private static final MethodHandle STRUCTOFOPAQUE_TAKE_IN;

    static {
        STRUCTOFOPAQUE_TAKE_IN = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("StructOfOpaque_take_in").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    public Opaque i;

    public OpaqueMut j;

    public StructOfOpaque() {
    }

    StructOfOpaque(Opaque i, OpaqueMut j) {
        this.i = i;
        this.j = j;
    }

    static StructOfOpaque fromNative(MemorySegment seg) {
        return new StructOfOpaque(
            new Opaque((MemorySegment) VH_I.get(seg, 0L)),
            new OpaqueMut((MemorySegment) VH_J.get(seg, 0L))
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_I.set(seg, 0L, this.i.handle);
        VH_J.set(seg, 0L, this.j.handle);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.i = new Opaque((MemorySegment) VH_I.get(seg, 0L));
        this.j = new OpaqueMut((MemorySegment) VH_J.get(seg, 0L));
    }

    public void takeIn(Opaque other) {
        try (var arena = Arena.ofConfined()) {
            var selfSeg = this.toNative(arena);
            STRUCTOFOPAQUE_TAKE_IN.invokeExact(selfSeg, other.handle);
            this.updateFromNative(selfSeg);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}