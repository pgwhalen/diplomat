package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrimitiveStructVecTest {
    @Test
    void testPushAndLen() {
        try (var vec = new PrimitiveStructVec()) {
            assertEquals(0, vec.len());

            PrimitiveStruct p = new PrimitiveStruct();
            p.x = 1.0f;
            p.a = true;
            p.b = 65;
            p.c = 100L;
            p.d = 200L;
            p.e = 42;
            vec.push(p);
            assertEquals(1, vec.len());

            PrimitiveStruct p2 = new PrimitiveStruct();
            p2.x = 2.0f;
            vec.push(p2);
            assertEquals(2, vec.len());
        }
    }

    @Test
    void testGet() {
        try (var vec = new PrimitiveStructVec()) {
            PrimitiveStruct p = new PrimitiveStruct();
            p.x = 3.14f;
            p.a = true;
            p.b = 66;
            p.c = 300L;
            p.d = 400L;
            p.e = 55;
            vec.push(p);

            PrimitiveStruct got = vec.get(0);
            assertEquals(3.14f, got.x, 0.001f);
            assertTrue(got.a);
            assertEquals(66, got.b);
            assertEquals(300L, got.c);
            assertEquals(400L, got.d);
            assertEquals(55, got.e);
        }
    }

    @Test
    void testAsSlice() {
        try (var vec = new PrimitiveStructVec()) {
            PrimitiveStruct p1 = new PrimitiveStruct();
            p1.x = 10.0f;
            p1.a = false;
            vec.push(p1);

            PrimitiveStruct p2 = new PrimitiveStruct();
            p2.x = 20.0f;
            p2.a = true;
            vec.push(p2);

            PrimitiveStruct[] slice = vec.asSlice();
            assertEquals(2, slice.length);
            assertEquals(10.0f, slice[0].x, 0.001f);
            assertFalse(slice[0].a);
            assertEquals(20.0f, slice[1].x, 0.001f);
            assertTrue(slice[1].a);
        }
    }
}
