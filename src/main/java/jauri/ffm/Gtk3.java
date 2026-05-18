package jauri.ffm;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Gtk3 {

    // Static arena for upcall stubs — lives for JVM lifetime
    private static final Arena STUB_ARENA = Arena.ofShared();

    // Handles
    private static final MethodHandle GTK_INIT;
    private static final MethodHandle GTK_WINDOW_NEW;
    private static final MethodHandle GTK_WINDOW_SET_DEFAULT_SIZE;
    private static final MethodHandle GTK_WIDGET_SHOW_ALL;
    private static final MethodHandle GTK_MAIN;
    private static final MethodHandle GTK_MAIN_QUIT;
    private static final MethodHandle G_SIGNAL_CONNECT_DATA;
    private static final long QUIT_STUB_ADDRESS;

    // GTK constants
    public static final int GTK_WINDOW_TOPLEVEL = 0;

    static {
        try {
            SymbolLookup gtk = SymbolLookup.libraryLookup("libgtk-3.so", STUB_ARENA);
            SymbolLookup gobject = SymbolLookup.libraryLookup("libgobject-2.0.so", STUB_ARENA);
            Linker linker = Linker.nativeLinker();

            // x86-64 ABI: all pointers are 8-byte longs via JAVA_LONG
            FunctionDescriptor VOID       = FunctionDescriptor.ofVoid();
            FunctionDescriptor PTR        = FunctionDescriptor.of(ValueLayout.JAVA_LONG);
            FunctionDescriptor VOID_PTR   = FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG);
            FunctionDescriptor VOID_PTR_INT_INT = FunctionDescriptor.ofVoid(
                ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
            FunctionDescriptor PTR_PTR_PTR = FunctionDescriptor.of(
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG);
            FunctionDescriptor PTR_PTR_PTR_LONG_LONG_INT = FunctionDescriptor.of(
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT);

            GTK_INIT = linker.downcallHandle(
                gtk.findOrThrow("gtk_init"),
                FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));

            GTK_WINDOW_NEW = linker.downcallHandle(
                gtk.findOrThrow("gtk_window_new"),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));

            GTK_WINDOW_SET_DEFAULT_SIZE = linker.downcallHandle(
                gtk.findOrThrow("gtk_window_set_default_size"),
                VOID_PTR_INT_INT);

            GTK_WIDGET_SHOW_ALL = linker.downcallHandle(
                gtk.findOrThrow("gtk_widget_show_all"),
                VOID_PTR);

            GTK_MAIN = linker.downcallHandle(
                gtk.findOrThrow("gtk_main"),
                VOID);

            GTK_MAIN_QUIT = linker.downcallHandle(
                gtk.findOrThrow("gtk_main_quit"),
                VOID);

            G_SIGNAL_CONNECT_DATA = linker.downcallHandle(
                gobject.findOrThrow("g_signal_connect_data"),
                PTR_PTR_PTR_LONG_LONG_INT);

            // Create upcall stub for "destroy" → gtk_main_quit
            MethodHandle quitHandle = MethodHandles.lookup().findStatic(
                Gtk3.class, "gtkMainQuit", MethodType.methodType(void.class));
            MemorySegment quitStub = linker.upcallStub(
                quitHandle, FunctionDescriptor.ofVoid(), STUB_ARENA);
            QUIT_STUB_ADDRESS = quitStub.address();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load GTK3 bindings", e);
        }
    }

    public static void gtkInit(long argc, long argv) {
        try { GTK_INIT.invokeExact(argc, argv); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static long gtkWindowNew(int type) {
        try { return (long) GTK_WINDOW_NEW.invokeExact(type); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void gtkWindowSetDefaultSize(long window, int width, int height) {
        try { GTK_WINDOW_SET_DEFAULT_SIZE.invokeExact(window, width, height); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void gtkWidgetShowAll(long widget) {
        try { GTK_WIDGET_SHOW_ALL.invokeExact(widget); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void gtkMain() {
        try { GTK_MAIN.invokeExact(); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void gtkMainQuit() {
        try { GTK_MAIN_QUIT.invokeExact(); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    /** Connect the "destroy" signal on a widget to call gtk_main_quit. */
    public static void gSignalConnectDestroy(long widget) {
        try {
            try (var inner = Arena.ofConfined()) {
                MemorySegment cSignal = inner.allocateFrom("destroy");
                // g_signal_connect_data returns gulong (handler ID)
                long handlerId = (long) G_SIGNAL_CONNECT_DATA.invokeExact(
                    widget,                    // instance
                    cSignal.address(),         // detailed_signal
                    QUIT_STUB_ADDRESS,         // c_handler (GCallback)
                    0L,                        // data (NULL)
                    0L,                        // destroy_data (NULL)
                    0);                        // connect_flags
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to connect destroy signal", t);
        }
    }
}
