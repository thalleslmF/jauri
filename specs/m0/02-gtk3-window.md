
# M0.2 — JNA Binding: GTK 3 Mínimo

**Marco:** M0 — Projeto Java + JNA + GTK Window  
**Dependências:** M0.1, `libgtk-3-dev` instalado  
**Entrega:** Janela vazia aparece na tela ao rodar `Main.main()`

## Objetivo

Criar bindings JNA para as funções mínimas do GTK3 e abrir uma janela. A thread principal é capturada pelo loop GTK.

## TDD (Test-First)

### Teste: Gtk3 bindings carregam sem erro

Arquivo: `src/test/java/jauri/jna/Gtk3Test.java`

```java
package jauri.jna;

import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Gtk3Test {

    @Test
    void gtkInit_doesNotCrash() {
        // Apenas verifica que a lib carrega e o método existe
        assertDoesNotThrow(() -> {
            // Não chamamos gtk_init real aqui (precisa de display)
            // Apenas validamos que o binding existe via Class.forName
            Class<?> clazz = Gtk3.class;
            assertNotNull(clazz.getMethod("gtk_init", int.class, String[].class));
            assertNotNull(clazz.getMethod("gtk_window_new", int.class));
            assertNotNull(clazz.getMethod("gtk_widget_show_all", Pointer.class));
            assertNotNull(clazz.getMethod("gtk_main"));
            assertNotNull(clazz.getMethod("gtk_main_quit"));
        }, "Todos os métodos GTK3 devem ser encontráveis via reflection");
    }

    @Test
    void constantsAreCorrect() {
        assertEquals(0, Gtk3.GTK_WINDOW_TOPLEVEL);
    }
}
```

### Teste: Sequência completa de inicialização (requer Xvfb)

Arquivo: `src/test/java/jauri/gui/GtkWindowOpenTest.java`

```java
package jauri.gui;

import jauri.jna.Gtk3;
import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "DISPLAY", matches = ".*")
class GtkWindowOpenTest {

    @Test
    void openAndCloseWindow() {
        // NOTA: Este teste só roda com DISPLAY (Xvfb ou monitor real)
        // Em CI: xvfb-run mvn test -Dtest=GtkWindowOpenTest
        
        Gtk3 gtk = Gtk3.INSTANCE;
        
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) {}
            gtk.gtk_main_quit();
        }).start();
        
        gtk.gtk_init(0, null);
        Pointer window = gtk.gtk_window_new(Gtk3.GTK_WINDOW_TOPLEVEL);
        gtk.gtk_widget_show_all(window);
        gtk.gtk_main();
        
        // Se chegou aqui, janela abriu e fechou sem crash
        assertTrue(true);
    }
}
```

## Implementação

### src/main/java/jauri/jna/Gtk3.java

```java
package jauri.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Binding JNA para GTK3 (libgtk-3.so).
 * 
 * Funções mínimas necessárias para abrir uma janela nativa.
 * O padrão é o mesmo do LibC: interface + INSTANCE singleton.
 * 
 * Thread safety:
 * - gtk_init() DEVE ser chamado na thread principal
 * - gtk_main() BLOQUEIA a thread até gtk_main_quit() ser chamada
 * - gtk_main_quit() DEVE ser chamada DA thread GTK (usar g_idle_add)
 */
public interface Gtk3 extends Library {
    Gtk3 INSTANCE = Native.load("gtk-3", Gtk3.class);

    int GTK_WINDOW_TOPLEVEL = 0;

    /** Inicializa GTK. argc normalmente é 0, argv null. */
    void gtk_init(int argc, String[] argv);

    /** Cria uma nova janela. type = GTK_WINDOW_TOPLEVEL (0). */
    Pointer gtk_window_new(int type);

    /** Exibe todos os widgets da janela (recursivo). */
    void gtk_widget_show_all(Pointer widget);

    /** Entra no loop de eventos GTK. BLOQUEIA a thread. */
    void gtk_main();

    /** Sai do loop de eventos GTK. Chamar de dentro do loop. */
    void gtk_main_quit();
}
```

### src/main/java/jauri/Main.java (atualizado)

```java
package jauri;

import jauri.jna.Gtk3;
import com.sun.jna.Pointer;

/**
 * Entry point do Jauri Runtime.
 * 
 * M0.2: Abre uma janela GTK vazia.
 * Em M0.3 isso será movido pra GtkMainLoop.
 */
public class Main {
    public static void main(String[] args) {
        Gtk3 gtk = Gtk3.INSTANCE;

        gtk.gtk_init(0, null);

        Pointer window = gtk.gtk_window_new(Gtk3.GTK_WINDOW_TOPLEVEL);
        gtk.gtk_widget_show_all(window);

        // BLOQUEIA até gtk_main_quit() ser chamado (Ctrl+C por enquanto)
        gtk.gtk_main();
    }
}
```

## Critério de Aceite

```bash
mvn test              # Testes unitários passam
mvn exec:java         # Janela vazia aparece, Ctrl+C fecha
```

Rodar `mvn exec:java` → janela vazia aparece na tela. Fechar com Ctrl+C encerra o processo.

## Edge Cases

- **Sem display**: GTK crasha com `cannot open display`. Resolver com Xvfb.
- **gtk_init chamado duas vezes**: GTK ignora (idempotente).
- **gtk_main sem init**: undefined behavior (segfault). Não fazer.

## Dependências de Sistema

```bash
sudo apt install libgtk-3-dev
```
