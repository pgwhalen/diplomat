package dev.diplomattest.custom;

import java.lang.foreign.SymbolLookup;

public final class NativeLibLoader {
    private NativeLibLoader() {}

    public static SymbolLookup get() {
        System.loadLibrary("diplomat_feature_tests");
        return SymbolLookup.loaderLookup();
    }
}
