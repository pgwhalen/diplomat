package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class OptionString implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle OPTIONSTRING_NEW;
    private static final MethodHandle OPTIONSTRING_WRITE;
    static final StructLayout OPTIONSTRING_WRITE_RESULT = MemoryLayout.structLayout(
            ValueLayout.JAVA_BOOLEAN.withName("is_ok")
        );

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionString_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        OPTIONSTRING_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionString_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
        OPTIONSTRING_WRITE = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("OptionString_write").orElseThrow(),
            FunctionDescriptor.of(OPTIONSTRING_WRITE_RESULT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
    }

    final MemorySegment handle;

    OptionString(MemorySegment handle) {
        this.handle = handle;
    }

    @Override
    public void close() {
        try {
            DESTROY.invokeExact(handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Optional<OptionString> new_(String diplomatStr) {
        try (var arena = Arena.ofConfined()) {
            byte[] diplomatStrBytes = diplomatStr.getBytes(StandardCharsets.UTF_8);

            var diplomatStrSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, diplomatStrBytes);
            var diplomatStrSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(diplomatStrSlice, 0L, diplomatStrSeg);
            DiplomatLib.VH_SV_LEN.set(diplomatStrSlice, 0L, (long) diplomatStrBytes.length);
            var resultAddr = (MemorySegment) OPTIONSTRING_NEW.invokeExact(diplomatStrSlice);
            return resultAddr.equals(MemorySegment.NULL) ? Optional.empty() : Optional.of(new OptionString(resultAddr));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String write() {
        var write = DiplomatLib.createWrite();
        try (var arena = Arena.ofConfined()) {
            var result = (MemorySegment) OPTIONSTRING_WRITE.invokeExact((SegmentAllocator) arena, handle, write);
            var isOk = result.get(ValueLayout.JAVA_BOOLEAN, 0L);
            if (isOk) {
                return DiplomatLib.writeToString(write);
            } else {
                DiplomatLib.destroyWrite(write);
                throw new RuntimeException("Diplomat error");
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}