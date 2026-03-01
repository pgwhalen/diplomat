package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttrOpaque1RenamedTest {
    @Test
    void testCreate() {
        try (AttrOpaque1Renamed opaque = new AttrOpaque1Renamed()) {
            assertNotNull(opaque.handle);
        }
    }

    @Test
    void testMethodReturns77() {
        try (AttrOpaque1Renamed opaque = new AttrOpaque1Renamed()) {
            assertEquals((byte) 77, opaque.method_renamed());
        }
    }
}
