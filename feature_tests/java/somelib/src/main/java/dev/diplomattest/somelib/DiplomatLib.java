package dev.diplomattest.somelib;

import java.lang.foreign.*;

/** Diplomat runtime support. */
public final class DiplomatLib {
    private DiplomatLib() {}

    static final StructLayout DIPLOMAT_STRING_VIEW = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("data"),
        ValueLayout.JAVA_LONG.withName("len")
    );
}