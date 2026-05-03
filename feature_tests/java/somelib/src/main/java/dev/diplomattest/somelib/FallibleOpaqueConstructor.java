package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class FallibleOpaqueConstructor {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("x")
    );
    private static final VarHandle VH_X = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("x"));
    private static final MethodHandle FALLIBLEOPAQUECONSTRUCTOR_CTOR;
    static final StructLayout FALLIBLEOPAQUECONSTRUCTOR_CTOR_RESULT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(7)
        );

    static {
        FALLIBLEOPAQUECONSTRUCTOR_CTOR = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("FallibleOpaqueConstructor_ctor").orElseThrow(),
            FunctionDescriptor.of(FALLIBLEOPAQUECONSTRUCTOR_CTOR_RESULT)
        );
    }

    public int x;

    private FallibleOpaqueConstructor(Void _internal) {
    }

    public FallibleOpaqueConstructor() {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) FALLIBLEOPAQUECONSTRUCTOR_CTOR.invokeExact((SegmentAllocator) arena);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                var seg = result.asSlice(0L, FallibleOpaqueConstructor.LAYOUT.byteSize());
                this.x = (int) VH_X.get(seg, 0L);
            } else {
                throw new ResultOpaque(result.get(ValueLayout.ADDRESS, 0L));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static FallibleOpaqueConstructor fromNative(MemorySegment seg) {
        var result = new FallibleOpaqueConstructor((Void) null);
        result.x = (int) VH_X.get(seg, 0L);
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_X.set(seg, 0L, this.x);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.x = (int) VH_X.get(seg, 0L);
    }
}