package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SliceTest {

    @Test
    void testStructWithSlicesReturnLast() {
        // return_last writes the last char of `first` to a DiplomatWrite
        StructWithSlices s = new StructWithSlices();
        s.first = "hello";
        s.second = new short[]{1, 2, 3};

        String last = s.returnLast();
        assertEquals("o", last);
    }

    @Test
    void testStructWithSlicesSingleChar() {
        StructWithSlices s = new StructWithSlices();
        s.first = "x";
        s.second = new short[]{};

        String last = s.returnLast();
        assertEquals("x", last);
    }

    @Test
    void testFloat64VecAsSlice() {
        var vec = Float64Vec.i16(new short[]{1, 2, 3});
        double[] result = vec.asSlice();
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, result);
    }

    @Test
    void testFloat64VecBorrow() {
        var vec = Float64Vec.i16(new short[]{4, 5, 6});
        double[] result = vec.borrow();
        assertArrayEquals(new double[]{4.0, 5.0, 6.0}, result);
    }

    @Test
    void testFloat64VecAsSliceEmpty() {
        var vec = Float64Vec.i16(new short[]{});
        double[] result = vec.asSlice();
        assertArrayEquals(new double[]{}, result);
    }
}
