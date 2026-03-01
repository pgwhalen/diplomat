package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Utf16WrapTest {
    @Test
    void testFromUtf16() {
        try (Utf16Wrap wrap = new Utf16Wrap("hello")) {
            String debug = wrap.getDebugStr();
            // "hello" in UTF-16 is [104, 101, 108, 108, 111]
            assertEquals("[104, 101, 108, 108, 111]", debug);
        }
    }

    @Test
    void testFromUtf16Unicode() {
        // CJK character U+4F60 ("ni") - single UTF-16 code unit
        try (Utf16Wrap wrap = new Utf16Wrap("\u4F60")) {
            String debug = wrap.getDebugStr();
            assertEquals("[20320]", debug);
        }
    }
}
