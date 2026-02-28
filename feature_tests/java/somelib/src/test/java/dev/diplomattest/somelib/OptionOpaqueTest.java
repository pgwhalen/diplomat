package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OptionOpaqueTest {

    @Test
    void testNewReturnsSome() {
        var opt = OptionOpaque.new_(5);
        assertTrue(opt.isPresent());
        try (var o = opt.get()) {
            o.assertInteger(5);
        }
    }

    @Test
    void testNewNoneReturnsEmpty() {
        var opt = OptionOpaque.newNone();
        assertTrue(opt.isEmpty());
    }

    @Test
    void testReturnsSomeSelf() {
        try (var o = OptionOpaque.new_(10).orElseThrow()) {
            var some = o.returnsSomeSelf();
            assertTrue(some.isPresent());
        }
    }

    @Test
    void testReturnsNoneSelf() {
        try (var o = OptionOpaque.new_(10).orElseThrow()) {
            var none = o.returnsNoneSelf();
            assertTrue(none.isEmpty());
        }
    }

    @Test
    void testOptionOpaqueArgumentWithSome() {
        try (var o = OptionOpaque.new_(5).orElseThrow()) {
            assertTrue(OptionOpaque.optionOpaqueArgument(o));
        }
    }

    @Test
    void testOptionOpaqueArgumentWithNull() {
        assertFalse(OptionOpaque.optionOpaqueArgument(null));
    }
}
