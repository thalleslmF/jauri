package jauri;

/**
 * Jauri — M2: HTTP Server + IPC Bridge.
 * Opens a GTK window with WebKit webview, starts an HTTP server on localhost,
 * serves static files from resources, and exposes /api/invoke for IPC.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Jauri M2 — HTTP Server + IPC Bridge");

        // M2.4 — Create registry with default commands
        HandlerRegistry registry = new HandlerRegistry();
        registry.registerDefaults();

        // M2.1 + M2.2 + M2.3 — Create and start HTTP server
        JauriHttpServer httpServer = new JauriHttpServer(registry);
        httpServer.start();
        System.out.println("[Main] HTTP server on " + httpServer.getBaseUrl());

        // M2.5 — Open webview pointing at the HTTP server
        GtkMainLoop loop = new GtkMainLoop();
        loop.setLoadUri(httpServer.getBaseUrl() + "/index.html");

        Thread gtkThread = new Thread(() -> {
            loop.start();
        }, "gtk-loop");
        gtkThread.start();

        // Wait for GTK thread to finish (when window closes)
        try {
            gtkThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        httpServer.close();
        System.out.println("Jauri M2 done.");
    }
}
