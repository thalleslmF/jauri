package jauri;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

class M1EndToEndTest {

    @Test
    @EnabledIfSystemProperty(named = "jauri.e2e", matches = "true")
    void webViewLoadsHtmlAndJsReturnsContent() {
        System.out.println("[E2E] Starting M1 WebView test with JS validation...");

        GtkMainLoop loop = new GtkMainLoop();
        assertEquals("CREATED", loop.getState());

        // Set HTML to be loaded on the GTK main thread
        final String testHtml = "<html><body style='background:#1a1a2e; color:#00d4aa; "
            + "display:flex; align-items:center; justify-content:center; height:100vh; "
            + "font-family:sans-serif;'>"
            + "<h1 id='title'>Jauri ✓</h1>"
            + "<p id='sub'>E2E with JS validation</p>"
            + "</body></html>";
        loop.setLoadHtmlOnStart(testHtml);

        // Thread that waits 1s for render, then runs JS validation
        final String[] jsResult = {null};
        final String[] jsResultSub = {null};
        Thread validator = new Thread(() -> {
            try {
                Thread.sleep(1500); // wait for HTML to render
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[E2E] Running JS validation...");
            JauriWebView wv = loop.getWebView();
            if (wv != null) {
                jsResult[0] = wv.evaluateJavaScript(
                    "document.getElementById('title').textContent", 4000);
                jsResultSub[0] = wv.evaluateJavaScript(
                    "document.getElementById('sub').textContent", 4000);
                System.out.println("[E2E] JS title = '" + jsResult[0] + "'");
                System.out.println("[E2E] JS sub   = '" + jsResultSub[0] + "'");
            }
            // Stop the GTK main loop
            loop.stop();
        }, "e2e-validator");
        validator.start();

        System.out.println("[E2E] Calling loop.start() — blocks on gtk_main...");
        loop.start();

        System.out.println("[E2E] loop.start() returned, state: " + loop.getState());
        assertEquals("STOPPED", loop.getState());

        // Validate JS results
        assertNotNull(jsResult[0], "JS result for title should not be null (timeout?)");
        assertEquals("Jauri ✓", jsResult[0], "Title text should be 'Jauri ✓'");

        assertNotNull(jsResultSub[0], "JS result for subtitle should not be null");
        assertEquals("E2E with JS validation", jsResultSub[0], "Subtitle text should match");

        // Verify WebView handle is non-zero
        JauriWebView wv = loop.getWebView();
        assertNotNull(wv, "WebView should be created");
        assertTrue(wv.getHandle() != 0, "WebView handle should be non-zero");

        System.out.println("[E2E] M1 WebView + JS validation test PASSED ✓");
    }

    @Test
    void webKitBindingsLoad() {
        assertDoesNotThrow(() -> {
            Class<?> c = jauri.ffm.WebKit.class;
        });
    }

    @Test
    void jauriWebViewCreationFailsWithoutGtk() {
        JauriWebView wv = new JauriWebView();
        assertThrows(IllegalStateException.class, () -> {
            wv.loadHtml("<h1>test</h1>", null);
        }, "loadHtml before create() should throw IllegalStateException");
    }

    @Test
    void evaluateJavaScriptFailsWithoutGtk() {
        JauriWebView wv = new JauriWebView();
        assertThrows(IllegalStateException.class, () -> {
            wv.evaluateJavaScript("1+1");
        }, "evaluateJavaScript before create() should throw IllegalStateException");
    }
}
