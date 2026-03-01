package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CallbackHolderTest {
    @Test
    void testCallbackHolder() {
        try (CallbackHolder holder = new CallbackHolder(x -> x + 100)) {
            assertEquals(105, holder.call(5));
        }
    }
}
