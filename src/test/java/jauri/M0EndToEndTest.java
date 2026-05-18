package jauri;

import jauri.ffm.Gtk3;
import jauri.ffm.LibC;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

class M0EndToEndTest {

    @Test
    void libCWorks() {
        int pid = LibC.getpid();
        assertTrue(pid > 0, "getpid() failed, got: " + pid);
    }

    @Test
    void gtkBindingsLoad() {
        // Verifica que o static init do Gtk3 não lança exceção
        // (SymbolLookup + downcallHandle para libgtk-3.so e libgobject-2.0.so)
        // NÃO chama gtk_window_new — isso requer display X
        assertDoesNotThrow(() -> {
            Class<?> c = Gtk3.class;
            assertEquals(0, Gtk3.GTK_WINDOW_TOPLEVEL);
        });
    }

    @Test
    @EnabledIfSystemProperty(named = "jauri.e2e", matches = "true")
    void windowOpensAndCloses() {
        System.out.println("[E2E] Starting window test...");

        GtkMainLoop loop = new GtkMainLoop();
        assertEquals("CREATED", loop.getState());
        System.out.println("[E2E] Loop created, state: " + loop.getState());

        // Schedule gtk_main_quit from another thread after 3s (CI needs more time)
        Thread killer = new Thread(() -> {
            try {
                System.out.println("[E2E] Killer thread sleeping 3s...");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("[E2E] Killer thread interrupted");
                Thread.currentThread().interrupt();
            }
            System.out.println("[E2E] Calling loop.stop()...");
            loop.stop();
            System.out.println("[E2E] loop.stop() returned");
        }, "gtk-killer");
        killer.start();

        System.out.println("[E2E] Calling loop.start() — blocks on gtk_main...");
        // Blocks until stop() triggers gtk_main_quit
        loop.start();

        System.out.println("[E2E] loop.start() returned, state: " + loop.getState());
        assertEquals("STOPPED", loop.getState());
        System.out.println("[E2E] Test PASSED");
    }
}
