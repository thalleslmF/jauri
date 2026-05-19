package jauri;

public class Main {
    public static void main(String[] args) {
        System.out.println("Jauri M1 — WebKit WebView");

        GtkMainLoop loop = new GtkMainLoop();

        // Start GTK in a separate thread so we can call loadHtml after window is shown
        Thread gtkThread = new Thread(() -> {
            loop.start();
        }, "gtk-loop");

        gtkThread.start();

        // Give GTK time to init and show the window
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Load HTML into the webview
        JauriWebView wv = loop.getWebView();
        if (wv != null) {
            String html = "<html><body style='background:#0d1117; color:#c9d1d9; "
                + "display:flex; align-items:center; justify-content:center; "
                + "height:100vh; margin:0; font-family:sans-serif; flex-direction:column;'>"
                + "<h1 style='color:#58a6ff;'>Jauri ✓</h1>"
                + "<p>WebKitGTK via Panama FFM</p>"
                + "<p style='color:#8b949e; font-size:0.85rem;'>M1 — WebView carregando HTML</p>"
                + "</body></html>";
            wv.loadHtml(html, null);
            System.out.println("[Main] HTML loaded into WebView");
        }

        // Wait for the GTK thread to finish (when window closes)
        try {
            gtkThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Jauri M1 done.");
    }
}
