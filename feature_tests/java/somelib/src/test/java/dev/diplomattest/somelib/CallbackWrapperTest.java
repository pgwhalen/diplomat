package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CallbackWrapperTest {
    @Test
    void testMultiArgCallback() {
        int result = CallbackWrapper.testMultiArgCallback(i -> i + 2, 10);
        assertEquals(22, result);
    }

    @Test
    void testNoArgs() {
        int result = CallbackWrapper.testNoArgs(() -> {});
        assertEquals(-5, result);
    }

    @Test
    void testCbWithStruct() {
        int result = CallbackWrapper.testCbWithStruct(s -> s.x + s.y);
        assertEquals(6, result);
    }

    @Test
    void testMultipleCbArgs() {
        int result = CallbackWrapper.testMultipleCbArgs(() -> 10, x -> x * 2);
        assertEquals(20, result);
    }

    @Test
    void testStrCbArg() {
        int result = CallbackWrapper.testStrCbArg(s -> s.length());
        assertEquals(7, result);
    }

    @Test
    void testSliceCbArg() {
        byte[] input = new byte[]{1, 2, 3};
        CallbackWrapper.testSliceCbArg(input, arr -> {
            assertEquals(3, arr.length);
        });
    }
}
