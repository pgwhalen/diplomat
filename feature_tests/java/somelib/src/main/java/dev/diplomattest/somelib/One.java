package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class One implements AutoCloseable {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;

    private static final MethodHandle DESTROY;
    private static final MethodHandle ONE_TRANSITIVITY;
    private static final MethodHandle ONE_CYCLE;
    private static final MethodHandle ONE_MANY_DEPENDENTS;
    private static final MethodHandle ONE_RETURN_OUTLIVES_PARAM;
    private static final MethodHandle ONE_DIAMOND_TOP;
    private static final MethodHandle ONE_DIAMOND_LEFT;
    private static final MethodHandle ONE_DIAMOND_RIGHT;
    private static final MethodHandle ONE_DIAMOND_BOTTOM;
    private static final MethodHandle ONE_DIAMOND_AND_NESTED_TYPES;
    private static final MethodHandle ONE_IMPLICIT_BOUNDS;
    private static final MethodHandle ONE_IMPLICIT_BOUNDS_DEEP;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        DESTROY = LINKER.downcallHandle(
            LIB.find("One_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        ONE_TRANSITIVITY = LINKER.downcallHandle(
            LIB.find("One_transitivity").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        ONE_CYCLE = LINKER.downcallHandle(
            LIB.find("One_cycle").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        ONE_MANY_DEPENDENTS = LINKER.downcallHandle(
            LIB.find("One_many_dependents").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        ONE_RETURN_OUTLIVES_PARAM = LINKER.downcallHandle(
            LIB.find("One_return_outlives_param").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        ONE_DIAMOND_TOP = LINKER.downcallHandle(
            LIB.find("One_diamond_top").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        ONE_DIAMOND_LEFT = LINKER.downcallHandle(
            LIB.find("One_diamond_left").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        ONE_DIAMOND_RIGHT = LINKER.downcallHandle(
            LIB.find("One_diamond_right").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        ONE_DIAMOND_BOTTOM = LINKER.downcallHandle(
            LIB.find("One_diamond_bottom").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        ONE_DIAMOND_AND_NESTED_TYPES = LINKER.downcallHandle(
            LIB.find("One_diamond_and_nested_types").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        ONE_IMPLICIT_BOUNDS = LINKER.downcallHandle(
            LIB.find("One_implicit_bounds").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        ONE_IMPLICIT_BOUNDS_DEEP = LINKER.downcallHandle(
            LIB.find("One_implicit_bounds_deep").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    One(MemorySegment handle) {
        this.handle = handle;
    }

    @Override
    public void close() {
        try {
            DESTROY.invokeExact(handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static One transitivity(One hold, One nohold) {
        try {
            return new One((MemorySegment) ONE_TRANSITIVITY.invokeExact(hold.handle, nohold.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static One cycle(Two hold, One nohold) {
        try {
            return new One((MemorySegment) ONE_CYCLE.invokeExact(hold.handle, nohold.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static One manyDependents(One a, One b, Two c, Two d, Two nohold) {
        try {
            return new One((MemorySegment) ONE_MANY_DEPENDENTS.invokeExact(a.handle, b.handle, c.handle, d.handle, nohold.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static One returnOutlivesParam(Two hold, One nohold) {
        try {
            return new One((MemorySegment) ONE_RETURN_OUTLIVES_PARAM.invokeExact(hold.handle, nohold.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static One diamondTop(One top, One left, One right, One bottom) {
        try {
            return new One((MemorySegment) ONE_DIAMOND_TOP.invokeExact(top.handle, left.handle, right.handle, bottom.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static One diamondLeft(One top, One left, One right, One bottom) {
        try {
            return new One((MemorySegment) ONE_DIAMOND_LEFT.invokeExact(top.handle, left.handle, right.handle, bottom.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static One diamondRight(One top, One left, One right, One bottom) {
        try {
            return new One((MemorySegment) ONE_DIAMOND_RIGHT.invokeExact(top.handle, left.handle, right.handle, bottom.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static One diamondBottom(One top, One left, One right, One bottom) {
        try {
            return new One((MemorySegment) ONE_DIAMOND_BOTTOM.invokeExact(top.handle, left.handle, right.handle, bottom.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static One diamondAndNestedTypes(One a, One b, One c, One d, One nohold) {
        try {
            return new One((MemorySegment) ONE_DIAMOND_AND_NESTED_TYPES.invokeExact(a.handle, b.handle, c.handle, d.handle, nohold.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static One implicitBounds(One explicitHold, One implicitHold, One nohold) {
        try {
            return new One((MemorySegment) ONE_IMPLICIT_BOUNDS.invokeExact(explicitHold.handle, implicitHold.handle, nohold.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static One implicitBoundsDeep(One explicit, One implicit1, One implicit2, One nohold) {
        try {
            return new One((MemorySegment) ONE_IMPLICIT_BOUNDS_DEEP.invokeExact(explicit.handle, implicit1.handle, implicit2.handle, nohold.handle));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}