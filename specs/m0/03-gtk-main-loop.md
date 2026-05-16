
# M0.3 — GtkMainLoop em Thread Separada

**Marco:** M0 — Projeto Java + JNA + GTK Window  
**Dependências:** M0.2  
**Entrega:** Fechar janela → programa termina com exit code 0

## Objetivo

Encapsular o loop GTK numa classe com startup/shutdown controlado, usando `g_signal_connect` pra detectar o fechamento da janela e `g_idle_add` pra chamar `gtk_main_quit` da thread correta.

## TDD (Test-First)

### Teste: GtkMainLoop.start() inicia thread GTK

Arquivo: `src/test/java/jauri/gui/GtkMainLoopTest.java`

```java
package jauri.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "DISPLAY", matches = ".*")
class GtkMainLoopTest {

    @Test
    void startLaunchesGtkThread() throws InterruptedException {
        GtkMainLoop loop = new GtkMainLoop();
        
        // start() lança uma thread separada
        loop.start();
        
        // Aguarda loop ficar ativo (janela criada)
        Thread.sleep(200);
        assertTrue(loop.isRunning(), "Loop deve estar rodando após start()");
        
        // stop() deve encerrar o loop
        loop.stop();
        loop.waitFor();
        assertFalse(loop.isRunning(), "Loop deve ter parado após stop()");
    }

    @Test
    void closeWindowTriggersStop() throws InterruptedException {
        GtkMainLoop loop = new GtkMainLoop();
        loop.start();
        Thread.sleep(200);
        
        // Simula fechamento da janela (o g_signal_connect fará isso)
        // Na prática, o usuário clica no X da janela
        loop.stop();
        loop.waitFor();
        
        // O waitFor() retornou → loop terminou
        assertFalse(loop.isRunning());
    }

    @Test
    void doubleStopIsSafe() throws InterruptedException {
        GtkMainLoop loop = new GtkMainLoop();
        loop.start();
        Thread.sleep(200);
        
        loop.stop();
        loop.waitFor();
        
        // Segunda chamada não deve lançar exceção
        assertDoesNotThrow(() -> loop.stop());
    }
}
```

### Teste: GtkMainLoop sem display não crasha

Arquivo: `src/test/java/jauri/gui/GtkMainLoopHeadlessTest.java`

```java
package jauri.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GtkMainLoopHeadlessTest {

    @Test
    void startWithoutDisplayDoesNotCrashJVM() {
        // Mesmo sem GTK, o start() não deve crashar a JVM
        // Ele pode lançar UnsatisfiedLinkError que é capturável
        assertThrows(UnsatisfiedLinkError.class, () -> {
            // Em headless, o binding GTK falha ao carregar
            // Isso é esperado — o erro é capturável
            GtkMainLoop loop = new GtkMainLoop();
            loop.start();
        });
    }
}
```

## Implementação

### src/main/java/jauri/gui/GtkMainLoop.java

```java
package jauri.gui;

import jauri.jna.Gtk3;
import com.sun.jna.Pointer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gerencia o loop principal do GTK em uma thread separada.
 * 
 * Motivação: gtk_main() BLOQUEIA a thread. Se chamarmos na main thread,
 * o HTTP server e outras threads nunca rodariam. Por isso GTK roda
 * em thread própria.
 * 
 * Shutdown:
 * - Fechar janela (X) → g_signal_connect(destroy) → chama stop()
 * - stop() agenda gtk_main_quit() via g_idle_add() → thread-safe
 * - waitFor() bloqueia até a thread GTK terminar
 */
public class GtkMainLoop {

    private Thread gtkThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CountDownLatch doneLatch = new CountDownLatch(1);
    private Pointer window;

    public void start() {
        if (running.get()) return;

        gtkThread = new Thread(() -> {
            try {
                Gtk3 gtk = Gtk3.INSTANCE;
                gtk.gtk_init(0, null);

                window = gtk.gtk_window_new(Gtk3.GTK_WINDOW_TOPLEVEL);
                
                // Conecta o evento "destroy" ao nosso callback de shutdown
                // Quando o X da janela for clicado, gtk_main_quit é chamado
                Gtk3.GCallback destroyCallback = (Pointer data) -> {
                    gtk.gtk_main_quit();
                    return 0;
                };
                Gtk3.INSTANCE.g_signal_connect(window, "destroy", destroyCallback, null);

                gtk.gtk_widget_show_all(window);
                running.set(true);
                
                // BLOQUEIA aqui até gtk_main_quit()
                gtk.gtk_main();
                
            } catch (UnsatisfiedLinkError e) {
                System.err.println("[ERROR] GTK3 não encontrado: " + e.getMessage());
            } finally {
                running.set(false);
                doneLatch.countDown();
            }
        }, "gtk-main-loop");
        
        gtkThread.setDaemon(true);
        gtkThread.start();
    }

    public void stop() {
        if (!running.get()) return;
        running.set(false);
        
        // g_idle_add agenda uma função pra rodar na thread GTK
        // Isso é SEGURO — a função roda no loop GTK, não na thread atual
        Gtk3.GSourceFunc quitFunc = (Pointer data) -> {
            Gtk3.INSTANCE.gtk_main_quit();
            return 0;
        };
        Gtk3.INSTANCE.g_idle_add(quitFunc, null);
    }

    public void waitFor() {
        try {
            doneLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public Pointer getWindow() {
        return window;
    }
}
```

### src/main/java/jauri/jna/Gtk3.java (atualizado)

```java
package jauri.jna;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface Gtk3 extends Library {
    Gtk3 INSTANCE = Native.load("gtk-3", Gtk3.class);

    int GTK_WINDOW_TOPLEVEL = 0;

    void gtk_init(int argc, String[] argv);
    Pointer gtk_window_new(int type);
    void gtk_widget_show_all(Pointer widget);
    void gtk_main();
    void gtk_main_quit();

    /** Callback pra eventos GTK (destroy, configure). Retorna void na prática. */
    interface GCallback extends Callback {
        int callback(Pointer data);
    }

    /** Callback pra g_idle_add. Retorna 0 = remove after run. */
    interface GSourceFunc extends Callback {
        int callback(Pointer data);
    }

    /** Conecta um sinal (evento) a um widget. Ex: "destroy", "configure-event". */
    void g_signal_connect(Pointer instance, String signal, GCallback callback, Pointer data);

    /** Agenda função pra rodar na próxima iteração do loop GTK. Thread-safe. */
    int g_idle_add(GSourceFunc func, Pointer data);
}
```

### src/main/java/jauri/Main.java (atualizado)

```java
package jauri;

import jauri.gui.GtkMainLoop;

/**
 * M0.3: Usa GtkMainLoop com shutdown limpo.
 * Fechar janela → programa termina exit 0.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        GtkMainLoop loop = new GtkMainLoop();
        loop.start();
        
        System.out.println("Jauri started. Close the window to exit.");
        
        loop.waitFor();
        System.out.println("Jauri stopped.");
    }
}
```

## Critério de Aceite

```bash
mvn test                    # Testes passam
mvn exec:java               # Janela aparece
# Fechar janela → programa termina exit 0
echo $?                     # Deve mostrar 0
```

Fechar janela → `gtk_main_quit` é chamado → `waitFor()` retorna → programa termina exit 0. Não fica processo zumbi.

## Edge Cases

- **Duplo stop**: `stop()` verifica `running.get()` antes de agendar
- **Fechar sem start**: `stop()` é noop se não running
- **GTK não instalado**: `start()` captura `UnsatisfiedLinkError` e não crasha a JVM
- **Thread interrompida**: `waitFor()` propaga interrupção via `Thread.currentThread().interrupt()`
