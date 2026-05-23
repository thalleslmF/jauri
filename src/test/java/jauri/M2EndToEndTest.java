package jauri;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * M2 E2E — validates the full integration:
 * 1. HTTP server starts and responds to /health
 * 2. /api/invoke ping returns pong
 * 3. Static index.html is served
 * 4. GTK window opens with webview pointing at HTTP server
 * 5. JS in the webview can call /api/invoke and display result
 **/
class M2EndToEndTest {

    @Test
    @EnabledIfSystemProperty(named = "jauri.e2e", matches = "true")
    void httpServerHealthAndPing() throws Exception {
        System.out.println("[E2E-M2] Starting HTTP server + IPC bridge test...");

        HandlerRegistry registry = new HandlerRegistry();
        registry.registerDefaults();

        JauriHttpServer httpServer = new JauriHttpServer(registry);
        httpServer.start();
        String baseUrl = httpServer.getBaseUrl();
        System.out.println("[E2E-M2] Server on " + baseUrl);

        var client = HttpClient.newHttpClient();

        // Verify /health
        HttpResponse<String> healthResp = client.send(
            HttpRequest.newBuilder().uri(URI.create(baseUrl + "/health")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, healthResp.statusCode());
        assertTrue(healthResp.body().contains("\"status\":\"ok\""),
            "Health endpoint should return status ok");

        // Verify /api/invoke ping
        HttpResponse<String> pingResp = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/invoke"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"cmd\":\"ping\",\"args\":{}}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pingResp.statusCode());
        assertTrue(pingResp.body().contains("\"result\":\"pong\""),
            "Ping should return pong");

        // Verify static index.html is served
        HttpResponse<String> indexResp = client.send(
            HttpRequest.newBuilder().uri(URI.create(baseUrl + "/index.html")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, indexResp.statusCode());
        assertTrue(indexResp.body().contains("Jauri ✓"),
            "index.html should contain Jauri ✓");
        assertTrue(indexResp.body().contains("/api/invoke"),
            "index.html should reference /api/invoke endpoint");

        // Verify / redirects to index.html
        HttpResponse<String> rootResp = client.send(
            HttpRequest.newBuilder().uri(URI.create(baseUrl + "/")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, rootResp.statusCode());
        assertEquals(indexResp.body(), rootResp.body(),
            "/ should serve same content as /index.html");

        // Now opens GTK window with webview and validates JS IPC
        GtkMainLoop loop = new GtkMainLoop();
        loop.setLoadUri(baseUrl + "/index.html");

        final Throwable[] jsError = {null};
        Thread validator = new Thread(() -> {
            try {
                Thread.sleep(3000); // wait for render + network
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[E2E-M2] Running JS validation on webview...");
            JauriWebView wv = loop.getWebView();
            if (wv != null) {
                // Check page title
                String title = wv.evaluateJavaScript("document.title", 4000);
                System.out.println("[E2E-M2] document.title = '" + title + "'");
                if (title == null || !title.contains("Jauri")) {
                    jsError[0] = new AssertionError("Expected title to contain 'Jauri', got: " + title);
                }

                // Check the button text
                String btnText = wv.evaluateJavaScript(
                    "document.querySelector('button').textContent", 4000);
                System.out.println("[E2E-M2] button text = '" + btnText + "'");
                if (btnText == null || !btnText.contains("Ping")) {
                    jsError[0] = new AssertionError("Expected button to contain 'Ping', got: " + btnText);
                }

                // Fire async fetch — result stored in window global
                // (return value is null because webkit_web_view_run_javascript can't
                // await Promises — we read the side effect after the async resolves)
                wv.evaluateJavaScript(
                    "(async () => { " +
                    "  var r = await fetch('/api/invoke', {" +
                    "    method: 'POST'," +
                    "    headers: {'Content-Type': 'application/json'}," +
                    "    body: JSON.stringify({cmd:'ping', args:{}})" +
                    "  });" +
                    "  window.__pingResult = await r.text();" +
                    "  document.getElementById('result').textContent = window.__pingResult;" +
                    "})()", 1000);

                // Give the async fetch time to complete on the GTK main loop
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Read the DOM element that was updated by the async fetch
                String resultDom = wv.evaluateJavaScript(
                    "document.getElementById('result').textContent", 4000);
                System.out.println("[E2E-M2] result DOM content = '" + resultDom + "'");
                if (resultDom == null || !resultDom.contains("pong")) {
                    jsError[0] = new AssertionError("JS ping result should appear in DOM, got: " + resultDom);
                }
            } else {
                jsError[0] = new AssertionError("WebView is null");
            }
            loop.stop();
        }, "e2e-m2-validator");
        validator.start();

        System.out.println("[E2E-M2] Starting GTK main loop...");
        loop.start();
        assertEquals("STOPPED", loop.getState());

        httpServer.close();

        if (jsError[0] != null) {
            throw new RuntimeException("E2E JS validation failed", jsError[0]);
        }

        System.out.println("[E2E-M2] All checks PASSED ✓");
    }
}
