package os_p2.native_c;

import java.lang.invoke.*;
import java.lang.foreign.*;

public class filededup_h extends filededup_h$shared {

    filededup_h() {
    }

    static final Arena LIBRARY_ARENA = Arena.ofAuto();

    static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup
            .libraryLookup(System.mapLibraryName("filededup"), LIBRARY_ARENA)
            .or(SymbolLookup.loaderLookup())
            .or(Linker.nativeLinker().defaultLookup());

    public static final AddressLayout FILEDEDUP = filededup_h.C_POINTER;

    private static class FDInit {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(
                filededup_h.C_POINTER);

        public static final MemorySegment ADDR = SYMBOL_LOOKUP.findOrThrow("FDInit");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    public static FunctionDescriptor FDInit$descriptor() {
        return FDInit.DESC;
    }

    public static MethodHandle FDInit$handle() {
        return FDInit.HANDLE;
    }

    public static MemorySegment FDInit$address() {
        return FDInit.ADDR;
    }

    public static MemorySegment FDInit() {
        var mh$ = FDInit.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("FDInit");
            }
            return (MemorySegment) mh$.invokeExact();
        } catch (Error | RuntimeException ex) {
            throw ex;
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class FDCheck {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(
                filededup_h.C_INT,
                filededup_h.C_POINTER,
                filededup_h.C_POINTER);

        public static final MemorySegment ADDR = SYMBOL_LOOKUP.findOrThrow("FDCheck");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    public static FunctionDescriptor FDCheck$descriptor() {
        return FDCheck.DESC;
    }

    public static MethodHandle FDCheck$handle() {
        return FDCheck.HANDLE;
    }

    public static MemorySegment FDCheck$address() {
        return FDCheck.ADDR;
    }

    public static int FDCheck(MemorySegment fd, MemorySegment filepath) {
        var mh$ = FDCheck.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("FDCheck", fd, filepath);
            }
            return (int) mh$.invokeExact(fd, filepath);
        } catch (Error | RuntimeException ex) {
            throw ex;
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class FDDump {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(
                filededup_h.C_POINTER,
                filededup_h.C_POINTER,
                filededup_h.C_POINTER);

        public static final MemorySegment ADDR = SYMBOL_LOOKUP.findOrThrow("FDDump");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    public static FunctionDescriptor FDDump$descriptor() {
        return FDDump.DESC;
    }

    public static MethodHandle FDDump$handle() {
        return FDDump.HANDLE;
    }

    public static MemorySegment FDDump$address() {
        return FDDump.ADDR;
    }

    public static MemorySegment FDDump(MemorySegment fd, MemorySegment length) {
        var mh$ = FDDump.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("FDDump", fd, length);
            }
            return (MemorySegment) mh$.invokeExact(fd, length);
        } catch (Error | RuntimeException ex) {
            throw ex;
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
}
