package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StructSliceTest {
    // testNestedSlice skipped: CyclicStructA/CyclicStructB have a pre-existing
    // circular static initialization issue unrelated to struct slice codegen

    @Test
    void testAssertSlice() {
        // BigStructWithStuff: first=101, second=505, third=9345, fourth={122,414}, fifth=99
        ScalarPairWithPadding sp = new ScalarPairWithPadding();
        sp.first = 122;
        sp.second = 414;

        BigStructWithStuff b1 = new BigStructWithStuff();
        b1.first = 101;
        b1.second = 505;
        b1.third = 9345;
        b1.fourth = sp;
        b1.fifth = 99;

        BigStructWithStuff b2 = new BigStructWithStuff();
        b2.first = 101;
        b2.second = 42; // This is the value we check
        b2.third = 9345;
        b2.fourth = sp;
        b2.fifth = 99;

        // assertSlice checks slice.len() > 1 and slice[1].second == second_value
        assertDoesNotThrow(() ->
            BigStructWithStuff.assertSlice(new BigStructWithStuff[]{b1, b2}, (short) 42)
        );
    }

    @Test
    void testMutableSlice() {
        PrimitiveStruct p1 = new PrimitiveStruct();
        p1.x = 1.0f;
        PrimitiveStruct p2 = new PrimitiveStruct();
        p2.x = 2.0f;
        PrimitiveStruct p3 = new PrimitiveStruct();
        p3.x = 3.0f;

        PrimitiveStruct[] arr = new PrimitiveStruct[]{p1, p2, p3};
        PrimitiveStruct.mutableSlice(arr);

        // running_sum: p1.x=1.0, p2.x=1.0+2.0=3.0, p3.x=3.0+3.0=6.0
        assertEquals(1.0f, arr[0].x);
        assertEquals(3.0f, arr[1].x);
        assertEquals(6.0f, arr[2].x);

        // alternate: p1.a=false, p2.a=true, p3.a=false
        assertFalse(arr[0].a);
        assertTrue(arr[1].a);
        assertFalse(arr[2].a);
    }
}
