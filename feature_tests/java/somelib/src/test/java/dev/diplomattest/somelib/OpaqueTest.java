package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpaqueTest {
    @Test
    void testOpaqueCreate() {
        try (Opaque opaque = new Opaque()) {
            assertNotNull(opaque.handle);
        }
    }

    @Test
    void testOpaqueFromStr() {
        try (Opaque opaque = Opaque.fromStr("hello")) {
            assertNotNull(opaque.handle);
        }
    }

    @Test
    void testReturnsUsize() {
        assertEquals(412L, Opaque.returnsUsize());
    }
}
