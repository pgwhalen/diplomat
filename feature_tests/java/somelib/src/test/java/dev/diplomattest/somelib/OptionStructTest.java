package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OptionStructTest {
    @Test
    void testOptionFieldsEmpty() {
        // new MyStructContainingAnOption() returns both fields as None
        MyStructContainingAnOption s = new MyStructContainingAnOption();
        assertNull(s.a);
        assertNull(s.b);
    }

    @Test
    void testOptionFieldsFilled() {
        // MyStructContainingAnOption.filled() returns both fields as Some
        MyStructContainingAnOption s = MyStructContainingAnOption.filled();
        assertNotNull(s.a);
        assertNotNull(s.b);

        // Check the inner MyStruct has expected values
        assertEquals(17, s.a.a);
        assertTrue(s.a.b);
        assertEquals(MyEnum.B, s.a.g);

        // Check the inner DefaultEnum
        assertEquals(DefaultEnum.A, s.b);
    }
}
