package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MyStructTest {
    @Test
    void testStructNew() {
        MyStruct s = MyStruct.new_();
        assertEquals(17, s.a);
        assertTrue(s.b);
        assertEquals(209, s.c & 0xFF); // c is u8 mapped to byte, interpret unsigned
        assertEquals(1234L, s.d);
        assertEquals(5991, s.e);
        assertEquals('餐', (char) s.f);
    }

    @Test
    void testStructIntoA() {
        MyStruct s = MyStruct.new_();
        byte a = s.intoA();
        assertEquals(17, a);
    }

    @Test
    void testReturnsZstResult() {
        assertDoesNotThrow(() -> MyStruct.returnsZstResult());
    }

    @Test
    void testFailsZstResult() {
        assertThrows(RuntimeException.class, () -> MyStruct.failsZstResult());
    }
}
