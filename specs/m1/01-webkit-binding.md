
# M1.1 — JNA Binding: WebKitGTK Inicialização

**Marco:** M1 — Janela com WebView carregando HTML  
**Dependências:** M0.4, `libwebkit2gtk-4.1-dev` instalado  
**Entrega:** WebView vazia dentro da janela GTK

## TDD (Test-First)

### Teste: WebKit bindings carregam

Arquivo: `src/test/java/jauri/jna/WebKitTest.java`

```java
package jauri.jna;

import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebKitTest {
    @Test
    void webkitClassLoads() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = WebKit.class;
            assertNotNull(clazz.getMethod("webkit_web_view_new"));
        });
    }
}
```

### Teste: WebViewEngine cria dentro do container

Arquivo: `src/test/java/jauri/webview/WebViewEngineTest.java`

```java
package jauri.webview;

import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebViewEngineTest {
    @Test
    void createReturnsWebViewNullWithoutGtk() {
        // Sem GTK rodando, create() retorna null ou lança
        WebViewEngine engine = new WebViewEngine();
        assertNull(engine.create(new Pointer(0)),
            "Sem GTK, create deve retornar null");
    }
}
```

## Implementação

### src/main/java/jauri/jna/WebKit.java

```java
package jauri.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Binding JNA para WebKitGTK (libwebkit2gtk-4.1.so).
 * 
 * Funções mínimas para controlar uma WebView:
 * - webkit_web_view_new() → cria instância
 * - webkit_web_view_load_html() → carrega HTML inline
 * - webkit_web_view_load_uri() → carrega URL
 */
public interface WebKit extends Library {
    WebKit INSTANCE = Native.load("webkit2gtk-4.1", WebKit.class);

    /** Cria uma nova WebView. Retorna GtkWidget*. */
    Pointer webkit_web_view_new();
}
```

### src/main/java/jauri/webview/WebViewEngine.java

```java
package jauri.webview;

import jauri.jna.Gtk3;
import jauri.jna.WebKit;
import com.sun.jna.Pointer;

/**
 * Engine da WebView. Gerencia criação e ciclo da WebKitGTK.
 * 
 * Cada WebView é um GtkWidget que se torna filho de um container GTK.
 * O container (GtkBox) gerencia o layout.
 * 
 * Uso:
 *   WebViewEngine engine = new WebViewEngine();
 *   Pointer webView = engine.create(parentContainer);
 *   engine.loadHtml("<h1>Hello</h1>");
 */
public class WebViewEngine {

    private Pointer webView;

    /** Cria WebView como filho do container GTK. Retorna o widget webview. */
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

    public Pointer getWebView() {
        return webView;
    }
}
```

### src/main/java/jauri/jna/Gtk3.java (atualizado — add gtk_box_new, gtk_container_add)

```java
// Adicionar ao binding existente:
void gtk_container_add(Pointer container, Pointer widget);
Pointer gtk_box_new(int orientation, int spacing);
```

### Atualização no GtkMainLoop.java

```java
// Adicionar no método start(), antes de gtk_widget_show_all:
Gtk3 gtk = Gtk3.INSTANCE;
Pointer box = gtk.gtk_box_new(0, 0);  // GTK_ORIENTATION_VERTICAL
gtk.gtk_container_add(window, box);

WebViewEngine engine = new WebViewEngine();
webView = engine.create(box);
```

## Critério de Aceite

```bash
mvn test
mvn exec:java  # Janela abre com área branca (a webview vazia)
```

Janela abre com uma área branca (a webview) dentro. Não precisa carregar HTML ainda — só a webview vazia. 0 erros, 0 segfaults.
