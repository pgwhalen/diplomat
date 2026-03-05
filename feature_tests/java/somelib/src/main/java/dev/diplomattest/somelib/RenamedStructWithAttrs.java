package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class RenamedStructWithAttrs {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BOOLEAN.withName("a"),
        MemoryLayout.paddingLayout(3),
        ValueLayout.JAVA_INT.withName("b")
    );
    private static final VarHandle VH_A = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("a"));
    private static final VarHandle VH_B = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("b"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle NAMESPACE_STRUCTWITHATTRS_NEW_FALLIBLE;
    private static final MethodHandle NAMESPACE_STRUCTWITHATTRS_C;
    private static final MethodHandle NAMESPACE_STRUCTWITHATTRS_DEPRECATED;
    static final StructLayout NAMESPACE_STRUCTWITHATTRS_NEW_FALLIBLE_RESULT = MemoryLayout.structLayout(
            RenamedStructWithAttrs.LAYOUT.withName("union_val"),
            ValueLayout.JAVA_BOOLEAN.withName("is_ok"),
            MemoryLayout.paddingLayout(3)
        );

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        NAMESPACE_STRUCTWITHATTRS_NEW_FALLIBLE = LINKER.downcallHandle(
            LIB.find("namespace_StructWithAttrs_new_fallible").orElseThrow(),
            FunctionDescriptor.of(NAMESPACE_STRUCTWITHATTRS_NEW_FALLIBLE_RESULT, ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT)
        );
        NAMESPACE_STRUCTWITHATTRS_C = LINKER.downcallHandle(
            LIB.find("namespace_StructWithAttrs_c").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, RenamedStructWithAttrs.LAYOUT)
        );
        NAMESPACE_STRUCTWITHATTRS_DEPRECATED = LINKER.downcallHandle(
            LIB.find("namespace_StructWithAttrs_deprecated").orElseThrow(),
            FunctionDescriptor.ofVoid(RenamedStructWithAttrs.LAYOUT)
        );
    }

    public boolean a;

    public int b;

    public RenamedStructWithAttrs() {
    }

    public RenamedStructWithAttrs(boolean a, int b) {
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) NAMESPACE_STRUCTWITHATTRS_NEW_FALLIBLE.invokeExact((SegmentAllocator) arena, a, b);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 8L);
            if (isOk) {
                var seg = result.asSlice(0L, RenamedStructWithAttrs.LAYOUT.byteSize());
                this.a = (boolean) VH_A.get(seg, 0L);
                this.b = (int) VH_B.get(seg, 0L);
            } else {
                throw new RuntimeException("Diplomat error");
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    static RenamedStructWithAttrs fromNative(MemorySegment seg) {
        var result = new RenamedStructWithAttrs();
        result.a = (boolean) VH_A.get(seg, 0L);
        result.b = (int) VH_B.get(seg, 0L);
        return result;
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_A.set(seg, 0L, this.a);
        VH_B.set(seg, 0L, this.b);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.a = (boolean) VH_A.get(seg, 0L);
        this.b = (int) VH_B.get(seg, 0L);
    }

    public int c() {
        try (var arena = Arena.ofConfined()) {
            return (int) NAMESPACE_STRUCTWITHATTRS_C.invokeExact(this.toNative(arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void deprecated() {
        try (var arena = Arena.ofConfined()) {
            NAMESPACE_STRUCTWITHATTRS_DEPRECATED.invokeExact(this.toNative(arena));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}