package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocaleTest {
    @Test
    void testLocaleCreate() {
        try (Locale locale = Locale.new_("en-US")) {
            assertNotNull(locale.handle);
        }
    }
}
