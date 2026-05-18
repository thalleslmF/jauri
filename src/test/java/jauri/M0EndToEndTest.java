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
        // xvfb-run mvn test -Djauri.e2e=true -Dtest=M0EndToEndTest#windowOpensAndCloses
        GtkMainLoop loop = new GtkMainLoop();
        assertEquals("CREATED", loop.getState());

        // Schedule gtk_main_quit from another thread after 500ms
        // gtk_main_quit is thread-safe per GLib docs
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            loop.stop();
        }).start();

        // Blocks until stop() triggers gtk_main_quit
        loop.start();

        assertEquals("STOPPED", loop.getState());
    }
}
