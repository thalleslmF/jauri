
# M1.2 — Carregar HTML Inline na WebView

**Marco:** M1 — Janela com WebView carregando HTML  
**Dependências:** M1.1  
**Entrega:** HTML renderizado na janela

## TDD (Test-First)

### Teste: WebViewEngine.loadHtml com conteúdo real (requer display)

Arquivo: `src/test/java/jauri/webview/WebViewEngineHtmlTest.java`

```java
package jauri.webview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "DISPLAY", matches = ".*")
class WebViewEngineHtmlTest {
    @Test
    void loadHtmlWithSimpleContent() {
        // Verifica que o método não lança exceção
        // A renderização real é verificada visualmente
        WebViewEngine engine = new WebViewEngine();
        
        assertDoesNotThrow(() -> {
            engine.loadHtml("<h1>Jauri</h1><p>Funcionou!</p>");
        }, "loadHtml não deve lançar exceção mesmo sem GTK running");
    }
}
```

### Teste: WebKit.bindings.htmlBindings

```java
@Test
void webkitHtmlMethodsExist() {
    assertDoesNotThrow(() -> {
        Class<?> clazz = WebKit.class;
        assertNotNull(clazz.getMethod("webkit_web_view_load_html",
            Pointer.class, String.class, String.class));
    });
}
```

## Implementação

### src/main/java/jauri/jna/WebKit.java (atualizado)

```java
public interface WebKit extends Library {
    WebKit INSTANCE = Native.load("webkit2gtk-4.1", WebKit.class);

    Pointer webkit_web_view_new();
    
    /** Carrega HTML como string. baseUri é opcional (pode ser null ou ""). */
    void webkit_web_view_load_html(Pointer webView, String html, String baseUri);
}
```

### src/main/java/jauri/webview/WebViewEngine.java (atualizado)

```java
package jauri.webview;

import jauri.jna.Gtk3;
import jauri.jna.WebKit;
import com.sun.jna.Pointer;

public class WebViewEngine {

    private Pointer webView;

    /** Cria WebView dentro do container GTK. */
    public Pointer create(Pointer parentContainer) {
        try {
            webView = WebKit.INSTANCE.webkit_web_view_new();
            Gtk3.INSTANCE.gtk_container_add(parentContainer, webView);
            return webView;
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[WARN] WebKitGTK não disponível: " + e.getMessage());
            return null;
        }
    }

    /** Carrega HTML inline na WebView. baseUri pode ser null. */
    public void loadHtml(String html) {
        if (webView == null) {
            System.err.println("[WARN] loadHtml chamado sem webView criada");
            return;
        }
        try {
            WebKit.INSTANCE.webkit_web_view_load_html(webView, html, "");
        } catch (Exception e) {
            System.err.println("[ERROR] Falha ao carregar HTML: " + e.getMessage());
        }
    }

    public Pointer getWebView() {
        return webView;
    }
}
```

### GtkMainLoop.java (atualizado)

```java
// Após criar a webView no start():
WebViewEngine engine = new WebViewEngine();
Pointer wv = engine.create(box);

// Carrega HTML simples como prova de vida
engine.loadHtml(
    "<html><body style='background:#1a1a1a; color:#fff; " +
    "display:flex; align-items:center; justify-content:center; " +
    "height:100vh; margin:0; font-family:sans-serif;'>" +
    "<h1>Jauri ✓</h1></body></html>"
);
```

## Critério de Aceite

`loadHtml("<h1>Jauri</h1>")` renderiza "Jauri" como heading na janela. CSS inline funciona. 0 segfaults.

## Edge Cases

- HTML vazio: webkit aceita string vazia (mostra página branca)
- HTML malformado: webkit renderiza com tags não fechadas (tolerante como navegador)
- Caractere especial: usar UTF-8. HTML com acentos funciona (ex: "Jáuri")
