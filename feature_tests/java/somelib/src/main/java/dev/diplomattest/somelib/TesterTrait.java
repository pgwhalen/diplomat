package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public interface TesterTrait {
    int testTraitFn(int x);
    void testVoidTraitFn();
    int testStructTraitFn(TraitTestingStruct s);

    StructLayout VTABLE_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("destructor"),
        ValueLayout.JAVA_LONG.withName("size"),
        ValueLayout.JAVA_LONG.withName("alignment"),
        ValueLayout.ADDRESS.withName("run_testTraitFn_callback"),
        ValueLayout.ADDRESS.withName("run_testVoidTraitFn_callback"),
        ValueLayout.ADDRESS.withName("run_testStructTraitFn_callback")
    );
    StructLayout TRAIT_STRUCT_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("data"),
        VTABLE_LAYOUT.withName("vtable")
    );
    java.lang.invoke.VarHandle VH_DATA = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("data"));
    java.lang.invoke.VarHandle VH_DESTRUCTOR = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("vtable"), MemoryLayout.PathElement.groupElement("destructor"));
    java.lang.invoke.VarHandle VH_SIZE = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("vtable"), MemoryLayout.PathElement.groupElement("size"));
    java.lang.invoke.VarHandle VH_ALIGNMENT = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("vtable"), MemoryLayout.PathElement.groupElement("alignment"));
    java.lang.invoke.VarHandle VH_RUN_TEST_TRAIT_FN = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("vtable"), MemoryLayout.PathElement.groupElement("run_testTraitFn_callback"));
    java.lang.invoke.VarHandle VH_RUN_TEST_VOID_TRAIT_FN = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("vtable"), MemoryLayout.PathElement.groupElement("run_testVoidTraitFn_callback"));
    java.lang.invoke.VarHandle VH_RUN_TEST_STRUCT_TRAIT_FN = TRAIT_STRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("vtable"), MemoryLayout.PathElement.groupElement("run_testStructTraitFn_callback"));

    final class Statics {
        private Statics() {}
    private static int traitRunner_testTraitFn(MemorySegment data, int x) {
            TesterTrait impl_ = DiplomatLib.getCallback(data.address(), TesterTrait.class);
            return impl_.testTraitFn(x);
        }
    private static void traitRunner_testVoidTraitFn(MemorySegment data) {
            TesterTrait impl_ = DiplomatLib.getCallback(data.address(), TesterTrait.class);
            impl_.testVoidTraitFn();
        }
    private static int traitRunner_testStructTraitFn(MemorySegment data, MemorySegment s) {
            TesterTrait impl_ = DiplomatLib.getCallback(data.address(), TesterTrait.class);
            return impl_.testStructTraitFn(TraitTestingStruct.fromNative(s));
        }
        static final MethodHandle MH_TEST_TRAIT_FN;
        static final MemorySegment UPCALL_testTraitFn;
        static {
            try {
                MH_TEST_TRAIT_FN = MethodHandles.lookup().findStatic(
                    Statics.class, "traitRunner_testTraitFn",
                    MethodType.methodType(int.class, MemorySegment.class, int.class));
                UPCALL_testTraitFn = DiplomatLib.LINKER_SHARED.upcallStub(
                    MH_TEST_TRAIT_FN,
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
                    Arena.global());
            } catch (ReflectiveOperationException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
        static final MethodHandle MH_TEST_VOID_TRAIT_FN;
        static final MemorySegment UPCALL_testVoidTraitFn;
        static {
            try {
                MH_TEST_VOID_TRAIT_FN = MethodHandles.lookup().findStatic(
                    Statics.class, "traitRunner_testVoidTraitFn",
                    MethodType.methodType(void.class, MemorySegment.class));
                UPCALL_testVoidTraitFn = DiplomatLib.LINKER_SHARED.upcallStub(
                    MH_TEST_VOID_TRAIT_FN,
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                    Arena.global());
            } catch (ReflectiveOperationException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
        static final MethodHandle MH_TEST_STRUCT_TRAIT_FN;
        static final MemorySegment UPCALL_testStructTraitFn;
        static {
            try {
                MH_TEST_STRUCT_TRAIT_FN = MethodHandles.lookup().findStatic(
                    Statics.class, "traitRunner_testStructTraitFn",
                    MethodType.methodType(int.class, MemorySegment.class, MemorySegment.class));
                UPCALL_testStructTraitFn = DiplomatLib.LINKER_SHARED.upcallStub(
                    MH_TEST_STRUCT_TRAIT_FN,
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, TraitTestingStruct.LAYOUT),
                    Arena.global());
            } catch (ReflectiveOperationException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
    }

    static MemorySegment createNative(Object impl_, Arena arena) {
        long id = DiplomatLib.registerCallback(impl_);
        var seg = arena.allocate(TRAIT_STRUCT_LAYOUT);
        VH_DATA.set(seg, 0L, MemorySegment.ofAddress(id));
        VH_DESTRUCTOR.set(seg, 0L, DiplomatLib.DESTRUCTOR_STUB);
        VH_SIZE.set(seg, 0L, 0L);
        VH_ALIGNMENT.set(seg, 0L, 0L);
        VH_RUN_TEST_TRAIT_FN.set(seg, 0L, Statics.UPCALL_testTraitFn);
        VH_RUN_TEST_VOID_TRAIT_FN.set(seg, 0L, Statics.UPCALL_testVoidTraitFn);
        VH_RUN_TEST_STRUCT_TRAIT_FN.set(seg, 0L, Statics.UPCALL_testStructTraitFn);
        return seg;
    }
}
