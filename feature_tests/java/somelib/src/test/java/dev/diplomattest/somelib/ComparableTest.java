package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

class ComparableTest {
    @Test
    void testCompareTo() {
        try (var a = RenamedComparable.new_((byte) 1);
             var b = RenamedComparable.new_((byte) 5)) {
            assertTrue(a.compareTo(b) < 0);
            assertTrue(b.compareTo(a) > 0);
        }
    }

    @Test
    void testCompareToEqual() {
        try (var a = RenamedComparable.new_((byte) 3);
             var b = RenamedComparable.new_((byte) 3)) {
            assertEquals(0, a.compareTo(b));
        }
    }
}
