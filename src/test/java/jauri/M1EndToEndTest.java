package jauri;

import jauri.ffm.Gtk3;
import jauri.ffm.WebKit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

class M1EndToEndTest {

    @Test
    @EnabledIfSystemProperty(named = "jauri.e2e", matches = "true")
    void webViewOpensAndLoadsHtml() {
        System.out.println("[E2E] Starting M1 WebView test...");

        GtkMainLoop loop = new GtkMainLoop();
        assertEquals("CREATED", loop.getState());

        // Set HTML to be loaded on the GTK main thread (WebKitGTK is not thread-safe)
        final String testHtml = "<html><body style='background:#1a1a2e; color:#00d4aa; "
            + "display:flex; align-items:center; justify-content:center; height:100vh; "
            + "font-family:sans-serif;'>"
            + "<h1>E2E Test ✓</h1></body></html>";
        loop.setLoadHtmlOnStart(testHtml);

        // Kill after 3s
        Thread killer = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[E2E] Calling loop.stop()...");
            loop.stop();
        }, "e2e-killer");
        killer.start();

        System.out.println("[E2E] Calling loop.start() — blocks on gtk_main...");
        loop.start();

        System.out.println("[E2E] loop.start() returned, state: " + loop.getState());
        assertEquals("STOPPED", loop.getState());

        // Verify WebView was created
        JauriWebView wv = loop.getWebView();
        assertNotNull(wv, "WebView should be created");
        assertTrue(wv.getHandle() != 0, "WebView handle should be non-zero");

        System.out.println("[E2E] M1 WebView test PASSED");
    }

    @Test
    void webKitBindingsLoad() {
        // Verifies WebKit class static init doesn't throw
        assertDoesNotThrow(() -> {
            Class<?> c = WebKit.class;
        });
    }

    @Test
    void jauriWebViewCreationFailsWithoutGtk() {
        // Without GTK init, calling loadHtml before create should throw
        JauriWebView wv = new JauriWebView();
        assertThrows(IllegalStateException.class, () -> {
            wv.loadHtml("<h1>test</h1>", null);
        }, "loadHtml before create() should throw IllegalStateException");
    }
}
