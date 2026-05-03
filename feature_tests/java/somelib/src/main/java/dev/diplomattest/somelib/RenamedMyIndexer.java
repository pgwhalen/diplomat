package dev.diplomattest.somelib;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class RenamedMyIndexer implements AutoCloseable {

    private static final MethodHandle DESTROY;
    private static final MethodHandle NAMESPACE_MYINDEXER_NEW;

    static {
        DESTROY = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_MyIndexer_destroy").orElseThrow(),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
        NAMESPACE_MYINDEXER_NEW = DiplomatLib.LINKER_SHARED.downcallHandle(
            DiplomatLib.LIB.find("namespace_MyIndexer_new").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, DiplomatLib.DIPLOMAT_STRING_VIEW)
        );
    }

    final MemorySegment handle;

    RenamedMyIndexer(MemorySegment handle) {
        this.handle = handle;
    }

    public RenamedMyIndexer(String[] v) {
        try (var arena = Arena.ofConfined()) {
            MemorySegment vSeg = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW, v.length);
            for (int i = 0; i < v.length; i++) {
                byte[] vBytes_i = v[i].getBytes(StandardCharsets.UTF_8);
                var vData_i = arena.allocateFrom(ValueLayout.JAVA_BYTE, vBytes_i);
                long vOff = i * 16L;
                vSeg.set(ValueLayout.ADDRESS, vOff, vData_i);
                vSeg.set(ValueLayout.JAVA_LONG, vOff + 8L, (long) vBytes_i.length);
            }
            var vSlice = arena.allocate(DiplomatLib.DIPLOMAT_STRING_VIEW);
            DiplomatLib.VH_SV_DATA.set(vSlice, 0L, vSeg);
            DiplomatLib.VH_SV_LEN.set(vSlice, 0L, (long) v.length);
            this.handle = (MemorySegment) NAMESPACE_MYINDEXER_NEW.invokeExact(vSlice);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() {
        try {
            DESTROY.invokeExact(handle);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}