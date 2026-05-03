package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class ImmutableStructOfOpaque {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("i")
    );
    private static final VarHandle VH_I = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("i"));
    private static final MethodHandle IMMUTABLESTRUCTOFOPAQUE_TAKE_IN;

    static {
        IMMUTABLESTRUCTOFOPAQUE_TAKE_IN = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("ImmutableStructOfOpaque_take_in").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    public Opaque i;

    public ImmutableStructOfOpaque() {
    }

    ImmutableStructOfOpaque(Opaque i) {
        this.i = i;
    }

    static ImmutableStructOfOpaque fromNative(MemorySegment seg) {
        return new ImmutableStructOfOpaque(
            new Opaque((MemorySegment) VH_I.get(seg, 0L))
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_I.set(seg, 0L, this.i.handle);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.i = new Opaque((MemorySegment) VH_I.get(seg, 0L));
    }

    public String takeIn() {
        var write = DiplomatLib.createWrite();
        try (var arena = Arena.ofConfined()) {
            var selfSeg = this.toNative(arena);
            IMMUTABLESTRUCTOFOPAQUE_TAKE_IN.invokeExact(selfSeg, write);
            return DiplomatLib.writeToString(write);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}