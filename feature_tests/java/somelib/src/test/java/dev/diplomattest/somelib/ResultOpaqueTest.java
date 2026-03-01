package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultOpaqueTest {
    @Test
    void testNewSuccess() {
        try (ResultOpaque opaque = new ResultOpaque(42)) {
            assertNotNull(opaque);
        }
    }

    @Test
    void testNewFailingUnit() {
        assertThrows(RuntimeException.class, () -> ResultOpaque.newFailingUnit());
    }

    @Test
    void testNewFailingFoo() {
        assertThrows(RuntimeException.class, () -> ResultOpaque.failingFoo());
    }

    @Test
    void testNewFailingBar() {
        assertThrows(RuntimeException.class, () -> ResultOpaque.failingBar());
    }

    @Test
    void testNewFailingStruct() {
        try {
            ResultOpaque.failingStruct(109);
            fail("Expected ErrorStruct to be thrown");
        } catch (ErrorStruct e) {
            assertEquals(109, e.i);
            assertEquals(12, e.j);
        }
    }

    @Test
    void testNewInt() {
        int result = ResultOpaque.newInt(42);
        assertEquals(42, result);
    }

    @Test
    void testAssertInteger() {
        try (ResultOpaque opaque = new ResultOpaque(42)) {
            opaque.assertInteger(42);
        }
    }
}
