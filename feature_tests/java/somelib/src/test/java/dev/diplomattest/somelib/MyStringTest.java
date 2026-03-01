package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MyStringTest {
    @Test
    void testNewAndGetStr() {
        try (MyString s = new MyString("hello")) {
            assertEquals("hello", s.getStr());
        }
    }

    @Test
    void testSetStrAndGetStr() {
        try (MyString s = new MyString("hello")) {
            s.setStr("world");
            assertEquals("world", s.getStr());
        }
    }

    @Test
    void testUnicodeRoundTrip() {
        try (MyString s = new MyString("\u4F60\u597D\uD83D\uDE00")) {
            assertEquals("\u4F60\u597D\uD83D\uDE00", s.getStr());
        }
    }

    @Test
    void testStringTransform() {
        // string_transform is a no-op, so it returns an empty string
        String result = MyString.stringTransform("anything");
        assertEquals("", result);
    }
}
