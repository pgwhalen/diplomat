package dev.diplomattest.somelib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TraitWrapperTest {
    @Test
    void testWithTrait() {
        TesterTrait impl = new TesterTrait() {
            @Override
            public int testTraitFn(int x) {
                return x + 1;
            }

            @Override
            public void testVoidTraitFn() {
                // no-op
            }

            @Override
            public int testStructTraitFn(TraitTestingStruct s) {
                return s.x + s.y;
            }
        };

        int result = TraitWrapper.testWithTrait(impl, 5);
        assertEquals(6, result);
    }

    @Test
    void testTraitWithStruct() {
        TesterTrait impl = new TesterTrait() {
            @Override
            public int testTraitFn(int x) {
                return x;
            }

            @Override
            public void testVoidTraitFn() {
                // no-op
            }

            @Override
            public int testStructTraitFn(TraitTestingStruct s) {
                return s.x + s.y;
            }
        };

        int result = TraitWrapper.testTraitWithStruct(impl);
        assertEquals(6, result);
    }
}
