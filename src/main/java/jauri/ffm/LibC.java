package jauri.ffm;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class LibC {

    private static final MethodHandle GETPID;

    static {
        try {
            Arena arena = Arena.ofShared();
            SymbolLookup libc = SymbolLookup.libraryLookup("libc.so.6", arena);
            Linker linker = Linker.nativeLinker();

            GETPID = linker.downcallHandle(
                libc.findOrThrow("getpid"),
                FunctionDescriptor.of(ValueLayout.JAVA_INT)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load LibC bindings", e);
        }
    }

    public static int getpid() {
        try {
            return (int) GETPID.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
