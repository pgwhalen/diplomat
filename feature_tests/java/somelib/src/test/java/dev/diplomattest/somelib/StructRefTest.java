package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StructRefTest {
    @Test
    void testTakesMut() {
        MyStruct s = new MyStruct();
        MyStruct o = new MyStruct();
        // Before: s.a=17, o.c=209
        s.takesMut(o);
        // After: self.a = 0, o.c = 100
        assertEquals(0, s.a);
        assertEquals(100, o.c & 0xFF);
    }

    @Test
    void testTakesConst() {
        MyStruct s = new MyStruct();
        MyStruct o = new MyStruct();
        // Before: s.a=17
        s.takesConst(o);
        // After: o.c = self.a = 17
        assertEquals(17, s.a); // self unchanged (immutable ref)
        assertEquals(17, o.c);
    }

    @Test
    void testMutableRef() {
        PrimitiveStruct s = new PrimitiveStruct();
        s.a = true;
        s.d = 42;

        PrimitiveStruct a = new PrimitiveStruct();
        a.d = 99;

        s.mutableRef(a);
        // self.a = false, a.d = 1
        assertFalse(s.a);
        assertEquals(1, a.d);
    }
}
