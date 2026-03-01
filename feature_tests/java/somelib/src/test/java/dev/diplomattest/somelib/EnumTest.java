package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnumTest {
    @Test
    void testEnumValues() {
        assertEquals(-2, MyEnum.A.toNative());
        assertEquals(-1, MyEnum.B.toNative());
        assertEquals(0, MyEnum.C.toNative());
        assertEquals(1, MyEnum.D.toNative());
        assertEquals(2, MyEnum.E.toNative());
        assertEquals(3, MyEnum.F.toNative());
    }

    @Test
    void testEnumFromNative() {
        assertEquals(MyEnum.A, MyEnum.fromNative(-2));
        assertEquals(MyEnum.B, MyEnum.fromNative(-1));
        assertEquals(MyEnum.C, MyEnum.fromNative(0));
        assertEquals(MyEnum.D, MyEnum.fromNative(1));
    }

    @Test
    void testEnumFromNativeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> MyEnum.fromNative(99));
    }

    @Test
    void testEnumRoundTrip() {
        for (MyEnum v : MyEnum.values()) {
            assertEquals(v, MyEnum.fromNative(v.toNative()));
        }
    }

    @Test
    void testEnumSelfMethod() {
        // MyEnum.into_value returns self as i8
        assertEquals(-2, MyEnum.A.intoValue());
        assertEquals(-1, MyEnum.B.intoValue());
        assertEquals(0, MyEnum.C.intoValue());
        assertEquals(3, MyEnum.F.intoValue());
    }

    @Test
    void testEnumStaticMethod() {
        assertEquals(MyEnum.A, MyEnum.getA());
    }

    @Test
    void testContiguousEnum() {
        assertEquals(0, ContiguousEnum.C.toNative());
        assertEquals(1, ContiguousEnum.D.toNative());
        assertEquals(2, ContiguousEnum.E.toNative());
        assertEquals(3, ContiguousEnum.F.toNative());

        assertEquals(ContiguousEnum.C, ContiguousEnum.fromNative(0));
        assertEquals(ContiguousEnum.F, ContiguousEnum.fromNative(3));
    }

    @Test
    void testEnumAsStructField() {
        // MyStruct.new_() creates struct with g = MyEnum::B (discriminant -1)
        MyStruct s = MyStruct.new_();
        assertEquals(MyEnum.B, s.g);
    }

    @Test
    void testStructWithEnumFieldPassedToRust() {
        // assert_struct validates all fields including the enum field g
        MyStruct s = MyStruct.new_();
        try (Opaque o = Opaque.new_()) {
            o.assertStruct(s);
        }
    }

    @Test
    void testEnumErrorThrown() {
        // ResultOpaque.newFailingFoo should throw ErrorEnumException with FOO
        try {
            ResultOpaque.newFailingFoo();
            fail("Expected ErrorEnumException");
        } catch (ErrorEnumException e) {
            assertEquals(ErrorEnum.FOO, e.getValue());
        }
    }

    @Test
    void testEnumErrorBar() {
        try {
            ResultOpaque.newFailingBar();
            fail("Expected ErrorEnumException");
        } catch (ErrorEnumException e) {
            assertEquals(ErrorEnum.BAR, e.getValue());
        }
    }

    @Test
    void testEnumSuccessReturn() {
        // ResultOpaque.new_ with a valid value should succeed (error is ErrorEnum)
        try (ResultOpaque opaque = ResultOpaque.new_(42)) {
            assertNotNull(opaque);
        }
    }

    @Test
    void testEnumAsReturnType() {
        // newInEnumErr always returns Err(ResultOpaque), so it should throw
        assertThrows(ResultOpaque.class, () -> ResultOpaque.newInEnumErr(42));
    }
}
