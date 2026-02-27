package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttrOpaque1RenamedTest {
    @Test
    void testCreate() {
        try (AttrOpaque1Renamed opaque = AttrOpaque1Renamed.totally_not_new()) {
            assertNotNull(opaque.handle);
        }
    }

    @Test
    void testMethodReturns77() {
        try (AttrOpaque1Renamed opaque = AttrOpaque1Renamed.totally_not_new()) {
            assertEquals((byte) 77, opaque.method_renamed());
        }
    }
}
