
# M3.1 — Close Handler no GTK

**Marco:** M3 — Shutdown limpo + ciclo de vida  
**Dependências:** M0.4  
**Entrega:** Clicar X na janela → shutdown orquestrado

## TDD (Test-First)

### Teste: Destroy callback chama shutdown

```java
package jauri.gui;

import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WindowCloseTest {
    @Test
    void windowHasDestroySignalConnected() {
        // Verifica que o callback destroy está configurado
        // Teste indireto: ao criar Window, o signal destroy deve estar conectado
        Window w = new Window();
        // Não podemos testar GTK signal connections sem display,
        // mas podemos verificar que o callback não é null
        assertNotNull(w.getDestroyCallback());
    }
}
```

## Implementação

### Window.java (atualizado)

```java
package jauri.gui;

import jauri.jna.Gtk3;
import com.sun.jna.Pointer;

public class Window {
    private Pointer handle;
    private Runnable onClose;
    private Gtk3.GCallback destroyCallback;

    public Pointer create(String title, Runnable onCloseHandler) {
        this.onClose = onCloseHandler;
        Gtk3 gtk = Gtk3.INSTANCE;
        handle = gtk.gtk_window_new(Gtk3.GTK_WINDOW_TOPLEVEL);
        gtk.gtk_window_set_default_size(handle, 800, 600);
        gtk.gtk_window_set_title(handle, title);
        
        // Conecta ao sinal "destroy" (disparado quando usuário fecha a janela)
        destroyCallback = (Pointer data) -> {
            if (onClose != null) {
                onClose.run();  // Dispara shutdown do app
            }
            return 0;
        };
        gtk.g_signal_connect(handle, "destroy", destroyCallback, null);
        
        return handle;
    }

    public void close() {
        if (handle != null) {
            Gtk3.INSTANCE.gtk_widget_destroy(handle);
        }
    }

    public Pointer getHandle() { return handle; }
    public Gtk3.GCallback getDestroyCallback() { return destroyCallback; }
}
```

### JauriApp.java (atualizado — onClose conecta ao stop)

```java
// No método start():
Window windowMgr = new Window();
windowMgr.create("Jauri", () -> {
    System.out.println("[Jauri] Window closed by user. Shutting down...");
    this.stop();
});
```

## Critério de Aceite

Fechar janela (clicar X) → callback destroy dispara → `JauriApp.stop()` é chamado. Processo sai exit 0. Nada de zumbi.

## Edge Cases

- **Fechar múltiplas vezes**: `stop()` é idempotente (só executa se state == RUNNING)
- **Janela destruída antes do callback**: não ocorre — destroy é o próprio evento de fechamento
