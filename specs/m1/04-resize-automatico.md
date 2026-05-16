
# M1.4 — WebView com Resize Automático

**Marco:** M1 — Janela com WebView carregando HTML  
**Dependências:** M1.3  
**Entrega:** Redimensionar janela → conteúdo HTML se ajusta

## TDD (Test-First)

### Teste: Window.java configura tamanho e resize callback

Arquivo: `src/test/java/jauri/gui/WindowTest.java`

```java
package jauri.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WindowTest {
    @Test
    void defaultSizeIs800x600() {
        Window w = new Window();
        assertEquals(800, w.getDefaultWidth());
        assertEquals(600, w.getDefaultHeight());
    }
}
```

## Implementação

### src/main/java/jauri/gui/Window.java

```java
package jauri.gui;

import jauri.jna.Gtk3;
import com.sun.jna.Pointer;

/**
 * Gerencia a janela GTK: criação, tamanho, título, eventos.
 * 
 * Encapsula os detalhes de gtk_window_new, gtk_window_set_default_size,
 * e callbacks de resize (configure-event).
 */
public class Window {

    private Pointer handle;
    private int width = 800;
    private int height = 600;

    /** Cria a janela com tamanho padrão. Retorna o handle GTK. */
    public Pointer create(String title) {
        Gtk3 gtk = Gtk3.INSTANCE;
        handle = gtk.gtk_window_new(Gtk3.GTK_WINDOW_TOPLEVEL);
        gtk.gtk_window_set_default_size(handle, width, height);
        gtk.gtk_window_set_title(handle, title);
        
        // Conecta callback de resize (configure-event)
        Gtk3.GCallback resizeCb = (Pointer data) -> {
            // GtkAllocation é atualizado automaticamente pelo GTK
            // A WebView redimensiona junto com o container
            return 0;
        };
        gtk.g_signal_connect(handle, "configure-event", resizeCb, null);
        
        return handle;
    }

    public Pointer getHandle() { return handle; }
    public int getDefaultWidth() { return width; }
    public int getDefaultHeight() { return height; }
}
```

### Gtk3.java (atualizado — add window size/title methods)

```java
void gtk_window_set_default_size(Pointer window, int width, int height);
void gtk_window_set_title(Pointer window, String title);
```

### GtkMainLoop.java (atualizado)

```java
// Substituir a criação direta de janela pelo uso de Window:
Window windowMgr = new Window();
Pointer win = windowMgr.create("Jauri");

Pointer box = gtk.gtk_box_new(0, 0);
gtk.gtk_container_add(win, box);

WebViewEngine engine = new WebViewEngine();
Pointer wv = engine.create(box);
```

## Critério de Aceite

Arrastar borda da janela → conteúdo HTML redimensiona proporcionalmente. WebView ocupa 100% do espaço.

## Constantes

```java
int GTK_ORIENTATION_VERTICAL = 0;
int GTK_EXPAND = 1;
int GTK_FILL = 1;
```
