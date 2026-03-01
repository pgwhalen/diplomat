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
}
