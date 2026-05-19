package jauri;

import jauri.ffm.Gtk3;
import jauri.ffm.WebKit;

/**
 * JauriWebView — wrapper around WebKitGTK WebView via Panama FFM.
 *
 * Responsibilities:
 *   - Create WebView widget as child of a GTK container
 *   - Load HTML inline (loadHtml)
 *   - Load from URI (loadUri)
 *   - Auto-expand with container resize (hexpand + vexpand)
 */
public class JauriWebView {

    private long webView = 0;

    /** Creates a WebView inside the given GTK container. Returns the GtkWidget*. */
    public long create(long container) {
        webView = WebKit.webkitWebViewNew();
        if (webView == 0) {
            throw new RuntimeException("webkit_web_view_new returned NULL");
        }

        // Make the webview expand with its container (M1.4)
        Gtk3.gtkWidgetSetHexpand(webView, true);
        Gtk3.gtkWidgetSetVexpand(webView, true);

        Gtk3.gtkContainerAdd(container, webView);
        return webView;
    }

    /** Loads HTML inline. baseUri is optional — pass null to omit. */
    public void loadHtml(String html, String baseUri) {
        if (webView == 0) {
            throw new IllegalStateException("WebView not created. Call create(container) first.");
        }
        WebKit.webkitWebViewLoadHtml(webView, html, baseUri);
    }

    /** Loads a URI (file://, http://, etc). */
    public void loadUri(String uri) {
        if (webView == 0) {
            throw new IllegalStateException("WebView not created. Call create(container) first.");
        }
        WebKit.webkitWebViewLoadUri(webView, uri);
    }

    /** Returns the native WebKitWebView* pointer. */
    public long getHandle() {
        return webView;
    }
}
