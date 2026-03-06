package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OwnedSliceTest {

    @Test
    void testFloat64VecFromOwned() {
        // Float64Vec constructor takes Box<[f64]> (owned slice param)
        var vec = new Float64Vec(new double[]{1.0, 2.0, 3.0});
        double[] result = vec.asSlice();
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, result);
        vec.close();
    }

    @Test
    void testFloat64VecFromOwnedEmpty() {
        var vec = new Float64Vec(new double[]{});
        double[] result = vec.asSlice();
        assertArrayEquals(new double[]{}, result);
        vec.close();
    }

    @Test
    void testFloat64VecFromOwnedToString() {
        var vec = new Float64Vec(new double[]{1.5, 2.5});
        String s = vec.toString();
        assertEquals("[1.5, 2.5]", s);
        vec.close();
    }

    @Test
    void testMyStringNewOwned() {
        // MyString.newOwned takes Box<DiplomatStr> (owned string param)
        try (var s = MyString.newOwned("hello owned")) {
            assertEquals("hello owned", s.getStr());
        }
    }

    @Test
    void testMyStringNewOwnedEmpty() {
        try (var s = MyString.newOwned("")) {
            assertEquals("", s.getStr());
        }
    }
}
