package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

/**
 * Testing JS-specific layout/padding behavior
 * Also being used to test CPP backends taking structs with primitive values.
 */
public class BigStructWithStuff {

    static final StructLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("first"),
        MemoryLayout.paddingLayout(1),
        ValueLayout.JAVA_SHORT.withName("second"),
        ValueLayout.JAVA_SHORT.withName("third"),
        MemoryLayout.paddingLayout(2),
        ScalarPairWithPadding.LAYOUT.withName("fourth"),
        ValueLayout.JAVA_BYTE.withName("fifth"),
        MemoryLayout.paddingLayout(3)
    );
    private static final VarHandle VH_FIRST = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("first"));
    private static final VarHandle VH_SECOND = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("second"));
    private static final VarHandle VH_THIRD = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("third"));
    private static final VarHandle VH_FIFTH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fifth"));
    private static final long OFFSET_FOURTH = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("fourth"));

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB;
    private static final MethodHandle BIGSTRUCTWITHSTUFF_ASSERT_VALUE;
    private static final MethodHandle BIGSTRUCTWITHSTUFF_ASSERT_SLICE;

    static {
        System.loadLibrary("diplomat_feature_tests");
        LIB = SymbolLookup.loaderLookup();
        BIGSTRUCTWITHSTUFF_ASSERT_VALUE = LINKER.downcallHandle(
            LIB.find("BigStructWithStuff_assert_value").orElseThrow(),
            FunctionDescriptor.ofVoid(BigStructWithStuff.LAYOUT, ValueLayout.JAVA_SHORT)
        );
        BIGSTRUCTWITHSTUFF_ASSERT_SLICE = LINKER.downcallHandle(
            LIB.find("BigStructWithStuff_assert_slice").orElseThrow(),
            FunctionDescriptor.ofVoid(DiplomatLib.DIPLOMAT_STRING_VIEW, ValueLayout.JAVA_SHORT)
        );
    }

    public byte first;

    public short second;

    public short third;

    public ScalarPairWithPadding fourth;

    public byte fifth;

    public BigStructWithStuff() {
    }

    BigStructWithStuff(byte first, short second, short third, ScalarPairWithPadding fourth, byte fifth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
        this.fifth = fifth;
    }

    static BigStructWithStuff fromNative(MemorySegment seg) {
        return new BigStructWithStuff(
            (byte) VH_FIRST.get(seg, 0L),
            (short) VH_SECOND.get(seg, 0L),
            (short) VH_THIRD.get(seg, 0L),
            ScalarPairWithPadding.fromNative(seg.asSlice(OFFSET_FOURTH, ScalarPairWithPadding.LAYOUT.byteSize())),
            (byte) VH_FIFTH.get(seg, 0L)
        );
    }

    MemorySegment toNative(Arena arena) {
        var seg = arena.allocate(LAYOUT);
        VH_FIRST.set(seg, 0L, this.first);
        VH_SECOND.set(seg, 0L, this.second);
        VH_THIRD.set(seg, 0L, this.third);
        seg.asSlice(OFFSET_FOURTH, ScalarPairWithPadding.LAYOUT.byteSize()).copyFrom(this.fourth.toNative(arena));
        VH_FIFTH.set(seg, 0L, this.fifth);
        return seg;
    }

    void updateFromNative(MemorySegment seg) {
        this.first = (byte) VH_FIRST.get(seg, 0L);
        this.second = (short) VH_SECOND.get(seg, 0L);
        this.third = (short) VH_THIRD.get(seg, 0L);
        this.fourth = ScalarPairWithPadding.fromNative(seg.asSlice(OFFSET_FOURTH, ScalarPairWithPadding.LAYOUT.byteSize()));
        this.fifth = (byte) VH_FIFTH.get(seg, 0L);
    }

    public static void assertSlice(BigStructWithStuff[] slice, short secondValue) {
        try (var arena = Arena.ofConfined()) {
            MemorySegment sliceSeg = arena.allocate(BigStructWithStuff.LAYOUT, slice.length);
            for (int i = 0; i < slice.length; i++) {
                sliceSeg.asSlice(i * BigStructWithStuff.LAYOUT.byteSize(), BigStructWithStuff.LAYOUT.byteSize())
                    .copyFrom(slice[i].toNative(arena));
            }
            var sliceSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(sliceSlice, 0L, sliceSeg);
            DiplomatLib.VH_SV_LEN.set(sliceSlice, 0L, (long) slice.length);
            BIGSTRUCTWITHSTUFF_ASSERT_SLICE.invokeExact(sliceSlice, secondValue);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public void assertValue(short extraVal) {
        try (var arena = Arena.ofConfined()) {
            BIGSTRUCTWITHSTUFF_ASSERT_VALUE.invokeExact(this.toNative(arena), extraVal);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}