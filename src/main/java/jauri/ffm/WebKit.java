package jauri.ffm;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * Panama FFM bindings for WebKitGTK 4.1 (libwebkit2gtk-4.1.so).
 *
 * Provides downcall handles for:
 *   - webkit_web_view_new()         → create a WebView widget
 *   - webkit_web_view_load_html()   → load HTML inline
 *   - webkit_web_view_load_uri()    → load from file/http URI
 */
public class WebKit {

    private static final Arena STUB_ARENA = Arena.ofShared();

    private static final MethodHandle WEBKIT_WEB_VIEW_NEW;
    private static final MethodHandle WEBKIT_WEB_VIEW_LOAD_HTML;
    private static final MethodHandle WEBKIT_WEB_VIEW_LOAD_URI;

    static {
        try {
            SymbolLookup webkit = SymbolLookup.libraryLookup("libwebkit2gtk-4.1.so", STUB_ARENA);
            Linker linker = Linker.nativeLinker();

            WEBKIT_WEB_VIEW_NEW = linker.downcallHandle(
                webkit.findOrThrow("webkit_web_view_new"),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG));

            WEBKIT_WEB_VIEW_LOAD_HTML = linker.downcallHandle(
                webkit.findOrThrow("webkit_web_view_load_html"),
                FunctionDescriptor.ofVoid(
                    ValueLayout.JAVA_LONG,   // WebKitWebView* (web_view)
                    ValueLayout.JAVA_LONG,   // const gchar* (content)
                    ValueLayout.JAVA_LONG)); // const gchar* (base_uri)

            WEBKIT_WEB_VIEW_LOAD_URI = linker.downcallHandle(
                webkit.findOrThrow("webkit_web_view_load_uri"),
                FunctionDescriptor.ofVoid(
                    ValueLayout.JAVA_LONG,   // WebKitWebView* (web_view)
                    ValueLayout.JAVA_LONG)); // const gchar* (uri)

        } catch (Exception e) {
            throw new RuntimeException("Failed to load WebKitGTK bindings", e);
        }
    }

    /** Creates a new WebKitWebView. Returns GtkWidget* (pointer). */
    public static long webkitWebViewNew() {
        try {
            return (long) WEBKIT_WEB_VIEW_NEW.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException("webkit_web_view_new failed", t);
        }
    }

    /** Loads HTML string into the WebView. */
    public static void webkitWebViewLoadHtml(long webView, String html, String baseUri) {
        try (var arena = Arena.ofConfined()) {
            MemorySegment htmlSeg = arena.allocateFrom(html);
            long baseAddr = (baseUri != null && !baseUri.isEmpty())
                ? arena.allocateFrom(baseUri).address()
                : 0L;
            WEBKIT_WEB_VIEW_LOAD_HTML.invokeExact(webView, htmlSeg.address(), baseAddr);
        } catch (Throwable t) {
            throw new RuntimeException("webkit_web_view_load_html failed", t);
        }
    }

    /** Loads a URI (file:// or http://) into the WebView. */
    public static void webkitWebViewLoadUri(long webView, String uri) {
        try (var arena = Arena.ofConfined()) {
            MemorySegment uriSeg = arena.allocateFrom(uri);
            WEBKIT_WEB_VIEW_LOAD_URI.invokeExact(webView, uriSeg.address());
        } catch (Throwable t) {
            throw new RuntimeException("webkit_web_view_load_uri failed", t);
        }
    }
}
