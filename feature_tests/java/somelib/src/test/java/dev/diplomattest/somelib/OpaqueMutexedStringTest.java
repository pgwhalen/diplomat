package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpaqueMutexedStringTest {
    @Test
    void testFromUsize() {
        try (OpaqueMutexedString s = OpaqueMutexedString.fromUsize(42)) {
            assertNotNull(s.handle);
        }
    }

    @Test
    void testGetLenAndAdd() {
        try (OpaqueMutexedString s = OpaqueMutexedString.fromUsize(123)) {
            // "123" has length 3
            assertEquals(13L, s.getLenAndAdd(10));
        }
    }

    @Test
    void testChange() {
        try (OpaqueMutexedString s = OpaqueMutexedString.fromUsize(42)) {
            s.change(12345);
            // "12345" has length 5
            assertEquals(5L, s.getLenAndAdd(0));
        }
    }

    @Test
    void testToUnsignedFromUnsigned() {
        try (OpaqueMutexedString s = OpaqueMutexedString.fromUsize(1)) {
            assertEquals((short) 42, s.toUnsignedFromUnsigned((short) 42));
        }
    }
}
