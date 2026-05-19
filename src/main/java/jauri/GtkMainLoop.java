package jauri;

import jauri.ffm.Gtk3;

public class GtkMainLoop {

    public enum State { CREATED, RUNNING, STOPPED }

    private State state = State.CREATED;
    private long window = 0;
    private JauriWebView webView;
    private String loadHtmlOnStart = null;

    public String getState() {
        return state.name();
    }

    /** Set HTML to load in the WebView right before gtk_main blocks. */
    public void setLoadHtmlOnStart(String html) {
        this.loadHtmlOnStart = html;
    }

    public void start() {
        if (state == State.STOPPED) {
            throw new IllegalStateException("Cannot restart a stopped loop");
        }
        if (state == State.RUNNING) {
            throw new IllegalStateException("Loop is already running");
        }

        Gtk3.gtkInit(0, 0);

        window = Gtk3.gtkWindowNew(Gtk3.GTK_WINDOW_TOPLEVEL);
        if (window == 0) {
            throw new RuntimeException("Failed to create GTK window");
        }

        Gtk3.gtkWindowSetDefaultSize(window, 800, 600);

        // Create GtkBox container for layout
        long box = Gtk3.gtkBoxNew(0, 0); // GTK_ORIENTATION_VERTICAL
        Gtk3.gtkContainerAdd(window, box);

        // Create WebView inside the box
        webView = new JauriWebView();
        webView.create(box);

        // Connect "destroy" signal → calls gtk_main_quit
        Gtk3.gSignalConnectDestroy(window);

        Gtk3.gtkWidgetShowAll(window);

        state = State.RUNNING;

        // Load HTML on the GTK main thread (WebKitGTK is not thread-safe)
        if (loadHtmlOnStart != null) {
            webView.loadHtml(loadHtmlOnStart, null);
        }

        // Blocks until gtk_main_quit is called
        Gtk3.gtkMain();

        state = State.STOPPED;
    }

    public JauriWebView getWebView() {
        return webView;
    }

    public void stop() {
        if (state == State.RUNNING) {
            Gtk3.gtkMainQuit();
        }
        state = State.STOPPED;
    }
}
