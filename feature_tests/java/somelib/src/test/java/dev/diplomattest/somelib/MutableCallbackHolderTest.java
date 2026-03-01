package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MutableCallbackHolderTest {
    @Test
    void testMutableCallbackHolder() {
        try (MutableCallbackHolder holder = new MutableCallbackHolder(x -> x * 3)) {
            assertEquals(15, holder.call(5));
        }
    }
}
