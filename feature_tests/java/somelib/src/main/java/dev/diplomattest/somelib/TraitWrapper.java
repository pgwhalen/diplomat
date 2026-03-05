package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

public class TraitWrapper {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BOOLEAN.withName("cantBeEmpty")
    );
    private static final VarHandle VH_CANT_BE_EMPTY = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("cantBeEmpty"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle TRAITWRAPPER_TEST_WITH_TRAIT;
    private static final MethodHandle TRAITWRAPPER_TEST_TRAIT_WITH_STRUCT;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        TRAITWRAPPER_TEST_WITH_TRAIT = LINKER.downcallHandle(
            LIB.find("TraitWrapper_test_with_trait").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, TesterTrait.TRAIT_STRUCT_LAYOUT, ValueLayout.JAVA_INT)
        );
        TRAITWRAPPER_TEST_TRAIT_WITH_STRUCT = LINKER.downcallHandle(
            LIB.find("TraitWrapper_test_trait_with_struct").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, TesterTrait.TRAIT_STRUCT_LAYOUT)
        );
    }

    public boolean cantBeEmpty;

    public TraitWrapper() {
    }

    TraitWrapper(boolean cantBeEmpty) {
        this.cantBeEmpty = cantBeEmpty;
    }

    static TraitWrapper fromNative(MemorySegment seg) {
        return new TraitWrapper(
            (boolean) VH_CANT_BE_EMPTY.get(seg, 0L)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_CANT_BE_EMPTY.set(seg, 0L, this.cantBeEmpty);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.cantBeEmpty = (boolean) VH_CANT_BE_EMPTY.get(seg, 0L);
    }

    public static int testWithTrait(TesterTrait t, int x) {
        try (var arena = Arena.ofConfined()) {
            var tNative = TesterTrait.createNative(t, arena);
            return (int) TRAITWRAPPER_TEST_WITH_TRAIT.invokeExact(tNative, x);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int testTraitWithStruct(TesterTrait t) {
        try (var arena = Arena.ofConfined()) {
            var tNative = TesterTrait.createNative(t, arena);
            return (int) TRAITWRAPPER_TEST_TRAIT_WITH_STRUCT.invokeExact(tNative);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}