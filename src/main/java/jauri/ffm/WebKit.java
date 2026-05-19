package jauri.ffm;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Panama FFM bindings for WebKitGTK 4.1 (libwebkit2gtk-4.1.so)
 * and JavaScriptCore (libjavascriptcoregtk-4.1.so).
 *
 * Provides downcall handles for:
 *   - webkit_web_view_new()                    → create a WebView widget
 *   - webkit_web_view_load_html()              → load HTML inline
 *   - webkit_web_view_load_uri()               → load from file/http URI
 *   - webkit_web_view_run_javascript()         → execute JS asynchronously
 *   - webkit_web_view_run_javascript_finish()  → extract result from callback
 *   - webkit_javascript_result_get_js_value()  → get JSCValue from result
 *   - jsc_value_to_string()                    → convert JSCValue to C string
 *   - g_free()                                 → free allocated C string
 */
public class WebKit {

    private static final Arena STUB_ARENA = Arena.ofShared();

    private static final MethodHandle WEBKIT_WEB_VIEW_NEW;
    private static final MethodHandle WEBKIT_WEB_VIEW_LOAD_HTML;
    private static final MethodHandle WEBKIT_WEB_VIEW_LOAD_URI;
    private static final MethodHandle WEBKIT_WEB_VIEW_RUN_JAVASCRIPT;
    private static final MethodHandle WEBKIT_WEB_VIEW_RUN_JAVASCRIPT_FINISH;
    private static final MethodHandle WEBKIT_JAVASCRIPT_RESULT_GET_JS_VALUE;
    private static final MethodHandle JSC_VALUE_TO_STRING;
    private static final MethodHandle G_FREE;

    /** Upcall stub address for GAsyncReadyCallback. */
    private static final long JAVASCRIPT_CALLBACK_ADDRESS;

    /** Stores JS results keyed by WebView pointer. Used by evaluateJavaScript(). */
    private static final ConcurrentHashMap<Long, String> JAVASCRIPT_RESULTS = new ConcurrentHashMap<>();

    static {
        try {
            SymbolLookup webkit = SymbolLookup.libraryLookup("libwebkit2gtk-4.1.so", STUB_ARENA);
            SymbolLookup jscore = SymbolLookup.libraryLookup("libjavascriptcoregtk-4.1.so", STUB_ARENA);
            SymbolLookup glib = SymbolLookup.libraryLookup("libglib-2.0.so", STUB_ARENA);
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

            WEBKIT_WEB_VIEW_RUN_JAVASCRIPT = linker.downcallHandle(
                webkit.findOrThrow("webkit_web_view_run_javascript"),
                FunctionDescriptor.ofVoid(
                    ValueLayout.JAVA_LONG,   // WebKitWebView* (web_view)
                    ValueLayout.JAVA_LONG,   // const gchar* (script)
                    ValueLayout.JAVA_LONG,   // GCancellable* (cancellable, NULL=0)
                    ValueLayout.JAVA_LONG,   // GAsyncReadyCallback (callback)
                    ValueLayout.JAVA_LONG)); // gpointer (user_data)

            WEBKIT_WEB_VIEW_RUN_JAVASCRIPT_FINISH = linker.downcallHandle(
                webkit.findOrThrow("webkit_web_view_run_javascript_finish"),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,   // returns WebKitJavascriptResult*
                    ValueLayout.JAVA_LONG,   // WebKitWebView* (web_view)
                    ValueLayout.JAVA_LONG,   // GAsyncResult* (result)
                    ValueLayout.JAVA_LONG)); // GError** (error, NULL=0)

            WEBKIT_JAVASCRIPT_RESULT_GET_JS_VALUE = linker.downcallHandle(
                webkit.findOrThrow("webkit_javascript_result_get_js_value"),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,   // returns JSCValue*
                    ValueLayout.JAVA_LONG)); // WebKitJavascriptResult*

            JSC_VALUE_TO_STRING = linker.downcallHandle(
                jscore.findOrThrow("jsc_value_to_string"),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,   // returns gchar*
                    ValueLayout.JAVA_LONG)); // JSCValue*

            G_FREE = linker.downcallHandle(
                glib.findOrThrow("g_free"),
                FunctionDescriptor.ofVoid(
                    ValueLayout.JAVA_LONG)); // gpointer

            // Create upcall stub for GAsyncReadyCallback
            // void callback(GObject* source, GAsyncResult* res, gpointer user_data)
            MethodHandle callbackHandle = MethodHandles.lookup().findStatic(
                WebKit.class, "onJavaScriptReady",
                MethodType.methodType(void.class, long.class, long.class, long.class));
            MemorySegment callbackStub = linker.upcallStub(
                callbackHandle,
                FunctionDescriptor.ofVoid(
                    ValueLayout.JAVA_LONG,  // GObject* source_object
                    ValueLayout.JAVA_LONG,  // GAsyncResult* res
                    ValueLayout.JAVA_LONG), // gpointer user_data
                STUB_ARENA);
            JAVASCRIPT_CALLBACK_ADDRESS = callbackStub.address();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load WebKitGTK bindings", e);
        }
    }

    // ── Public API ────────────────────────────────────────────────

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

    /**
     * Executes JavaScript in the WebView asynchronously.
     * The callback fires on the GTK main loop thread.
     * Use getJavaScriptResult(webView) after a brief delay to retrieve the result.
     */
    public static void webkitWebViewRunJavaScript(long webView, String script) {
        try (var arena = Arena.ofConfined()) {
            MemorySegment scriptSeg = arena.allocateFrom(script);
            WEBKIT_WEB_VIEW_RUN_JAVASCRIPT.invokeExact(
                webView,          // WebKitWebView*
                scriptSeg.address(), // const gchar* script
                0L,               // GCancellable* (NULL)
                JAVASCRIPT_CALLBACK_ADDRESS, // GAsyncReadyCallback
                webView);         // user_data = webView pointer (used as key)
        } catch (Throwable t) {
            throw new RuntimeException("webkit_web_view_run_javascript failed", t);
        }
    }

    /** Retrieve the last JS result for a given WebView. Returns null if not yet available. */
    public static String getJavaScriptResult(long webView) {
        return JAVASCRIPT_RESULTS.get(webView);
    }

    /** Clear the JS result for a given WebView. */
    public static void clearJavaScriptResult(long webView) {
        JAVASCRIPT_RESULTS.remove(webView);
    }

    /**
     * Executes JavaScript and blocks until the result is available.
     * Polls at 50ms intervals up to the timeout.
     * Returns the result string, or null on timeout.
     */
    public static String webkitWebViewEvaluateJavaScript(long webView, String script, long timeoutMs) {
        JAVASCRIPT_RESULTS.remove(webView); // clear previous result
        webkitWebViewRunJavaScript(webView, script);
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String result = JAVASCRIPT_RESULTS.get(webView);
            if (result != null) {
                JAVASCRIPT_RESULTS.remove(webView);
                return result;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null; // timeout
    }

    // ── Internal callback ─────────────────────────────────────────

    /**
     * Upcall target for GAsyncReadyCallback.
     * Called on the GTK main loop thread when JS execution completes.
     * Extracts the JS result string and stores it in JAVASCRIPT_RESULTS.
     */
    private static void onJavaScriptReady(long sourceObject, long res, long userData) {
        try {
            // webkit_web_view_run_javascript_finish(web_view, result, &error)
            long jsResult = (long) WEBKIT_WEB_VIEW_RUN_JAVASCRIPT_FINISH.invokeExact(
                sourceObject, res, 0L);
            if (jsResult == 0) {
                return; // error — result stays null
            }

            long jsValue = (long) WEBKIT_JAVASCRIPT_RESULT_GET_JS_VALUE.invokeExact(jsResult);
            if (jsValue == 0) return;

            long strPtr = (long) JSC_VALUE_TO_STRING.invokeExact(jsValue);
            if (strPtr == 0) return;

            // Read the C string (JDK 24: ofAddress(long) → reinterpret for bounds)
            String result = MemorySegment.ofAddress(strPtr).reinterpret(65536).getString(0);

            // Free the string allocated by jsc_value_to_string
            G_FREE.invokeExact(strPtr);

            // Store result indexed by webView pointer (user_data)
            JAVASCRIPT_RESULTS.put(userData, result);

        } catch (Throwable t) {
            // Log but don't crash the GTK main loop
            System.err.println("[WebKit] onJavaScriptReady error: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
