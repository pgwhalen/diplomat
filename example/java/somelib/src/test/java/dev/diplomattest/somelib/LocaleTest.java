package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocaleTest {
    @Test
    void testLocaleCreate() {
        try (Locale locale = new Locale("en-US")) {
            assertNotNull(locale.handle);
        }
    }
}
