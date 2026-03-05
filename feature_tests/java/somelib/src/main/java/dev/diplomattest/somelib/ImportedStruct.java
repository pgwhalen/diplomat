package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class ImportedStruct {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("foo"),
        ValueLayout.JAVA_BYTE.withName("count"),
        MemoryLayout.paddingLayout(3)
    );
    private static final VarHandle VH_FOO = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("foo"));
    private static final VarHandle VH_COUNT = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("count"));

    public UnimportedEnum foo;

    public byte count;

    public ImportedStruct() {
    }

    ImportedStruct(UnimportedEnum foo, byte count) {
        this.foo = foo;
        this.count = count;
    }

    static ImportedStruct fromNative(MemorySegment seg) {
        return new ImportedStruct(
            UnimportedEnum.fromNative((int) VH_FOO.get(seg, 0L)),
            (byte) VH_COUNT.get(seg, 0L)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_FOO.set(seg, 0L, this.foo.toNative());
        VH_COUNT.set(seg, 0L, this.count);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.foo = UnimportedEnum.fromNative((int) VH_FOO.get(seg, 0L));
        this.count = (byte) VH_COUNT.get(seg, 0L);
    }
}