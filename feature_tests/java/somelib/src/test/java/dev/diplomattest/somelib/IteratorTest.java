package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

class IteratorTest {
    @Test
    void testOpaqueIterable() {
        try (RenamedOpaqueIterable iterable = new RenamedOpaqueIterable(3)) {
            List<AttrOpaque1Renamed> items = new ArrayList<>();
            for (AttrOpaque1Renamed item : iterable) {
                items.add(item);
            }
            assertEquals(3, items.size());
            // Clean up iterator-produced items
            for (AttrOpaque1Renamed item : items) {
                item.close();
            }
        }
    }

    @Test
    void testEmptyOpaqueIterable() {
        try (RenamedOpaqueIterable iterable = new RenamedOpaqueIterable(0)) {
            List<AttrOpaque1Renamed> items = new ArrayList<>();
            for (AttrOpaque1Renamed item : iterable) {
                items.add(item);
            }
            assertEquals(0, items.size());
        }
    }
}
