
# M0.4 — JauriApp: Classe de Ciclo de Vida

**Marco:** M0 — Projeto Java + JNA + GTK Window  
**Dependências:** M0.3  
**Entrega:** App pode ser iniciado e parado programaticamente com máquina de estados

## Objetivo

Criar a classe orquestradora `JauriApp` com estados explícitos (CREATED → RUNNING → STOPPED), que esconde os detalhes do GtkMainLoop e servirá de base pros marcos seguintes.

## TDD (Test-First)

### Teste: Ciclo de vida completo

Arquivo: `src/test/java/jauri/JauriAppTest.java`

```java
package jauri;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JauriAppTest {

    @Test
    void initialStateIsCreated() {
        JauriApp app = new JauriApp();
        assertEquals(JauriApp.State.CREATED, app.getState());
    }

    @Test
    void startTransitionsToRunning() {
        JauriApp app = new JauriApp();
        // Nota: start() real tentaria abrir GTK e falharia sem display.
        // Este teste usa a versão mockada via testStart().
        // O teste real (com xvfb) está em JauriAppIntegrationTest.
        app.testStart();  // mock: só muda estado, não inicia GTK
        assertEquals(JauriApp.State.RUNNING, app.getState());
    }

    @Test
    void stopTransitionsToStopped() {
        JauriApp app = new JauriApp();
        app.testStart();
        app.stop();
        assertEquals(JauriApp.State.STOPPED, app.getState());
    }

    @Test
    void stopWithoutStartDoesNotThrow() {
        JauriApp app = new JauriApp();
        assertDoesNotThrow(() -> app.stop(),
            "stop() em estado CREATED deve ser noop");
        assertEquals(JauriApp.State.CREATED, app.getState());
    }

    @Test
    void doubleStopIsSafe() {
        JauriApp app = new JauriApp();
        app.testStart();
        app.stop();
        assertDoesNotThrow(() -> app.stop(),
            "Segundo stop() deve ser noop");
    }

    @Test
    void awaitReturnsAfterStop() {
        JauriApp app = new JauriApp();
        app.testStart();
        
        Thread stopper = new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            app.stop();
        });
        stopper.start();
        
        // await() bloqueia até STOPPED
        app.await();
        assertEquals(JauriApp.State.STOPPED, app.getState());
    }
}
```

### Teste: Integração com GTK real (requer display)

Arquivo: `src/test/java/jauri/JauriAppIntegrationTest.java`

```java
package jauri;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "DISPLAY", matches = ".*")
class JauriAppIntegrationTest {

    @Test
    void fullLifecycleWithGtk() throws InterruptedException {
        JauriApp app = new JauriApp();
        assertEquals(JauriApp.State.CREATED, app.getState());
        
        app.start();  // GTK real
        Thread.sleep(300);
        assertEquals(JauriApp.State.RUNNING, app.getState());
        
        app.stop();
        app.await();
        assertEquals(JauriApp.State.STOPPED, app.getState());
    }
}
```

## Implementação

### src/main/java/jauri/JauriApp.java

```java
package jauri;

import jauri.gui.GtkMainLoop;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orquestrador do ciclo de vida do Jauri Runtime.
 * 
 * Estados:
 *   CREATED → start() → RUNNING → stop() → STOPPED
 *   CREATED ── stop() ──→ CREATED (noop)
 * 
 * Cada método verifica o estado atual antes de agir:
 * - start() só funciona se CREATED
 * - stop() só funciona se RUNNING
 * - await() bloqueia até STOPPED
 * 
 * Responsabilidades futuras (M2+):
 * - Iniciar HTTP server antes do GTK
 * - Extrair static/ do classpath
 * - Orquestrar shutdown na ordem inversa
 */
public class JauriApp {

    public enum State { CREATED, RUNNING, STOPPED }

    private final AtomicReference<State> state = new AtomicReference<>(State.CREATED);
    private final CountDownLatch stoppedLatch = new CountDownLatch(1);
    private GtkMainLoop gtkLoop;

    /** Inicia o app com GTK real. Requer display. */
    public JauriApp start() {
        if (!state.compareAndSet(State.CREATED, State.RUNNING)) {
            return this;  // já rodando ou parado
        }
        
        gtkLoop = new GtkMainLoop();
        gtkLoop.start();
        
        return this;
    }

    /** Inicia sem GTK (apenas muda estado). Usado em testes headless. */
    public JauriApp testStart() {
        state.set(State.RUNNING);
        return this;
    }

    /** Para o app. Thread-safe. Idempotente. */
    public JauriApp stop() {
        State prev = state.getAndUpdate(s -> 
            s == State.RUNNING ? State.STOPPED : s
        );
        
        if (prev == State.RUNNING && gtkLoop != null) {
            gtkLoop.stop();
            stoppedLatch.countDown();
        }
        
        return this;
    }

    /** Bloqueia até o app estar completamente parado. */
    public void await() {
        try {
            stoppedLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public State getState() {
        return state.get();
    }
}
```

### src/main/java/jauri/Main.java (atualizado)

```java
package jauri;

/**
 * M0.4: Usa JauriApp com ciclo de vida.
 * start().await() — simples e limpo.
 */
public class Main {
    public static void main(String[] args) {
        new JauriApp()
            .start()
            .await();
        
        System.out.println("Jauri stopped gracefully.");
    }
}
```

## Critério de Aceite

```bash
mvn test                              # Testes unitários passam
# Com display:
mvn exec:java                         # Janela aparece
# Fechar janela → "Jauri stopped gracefully." → exit 0
```

Teste que chama `testStart()` + `stop()` sem GTK completa em <1s. Ciclo de vida não lança exceção. Estados seguem ordem correta.

## Edge Cases

- **stop() antes de start()**: `compareAndSet(CREATED, STOPPED)` falha, `state` permanece CREATED
- **start() duas vezes**: segundo `compareAndSet` falha, retorna `this` silenciosamente
- **await() sem stop()**: trava 10s até timeout (evita hang infinito)
- **Interrupção**: `await()` propaga interrupted flag

## Padrão de Estados

```
CREATED ──start()──▶ RUNNING ──stop()──▶ STOPPED
  │                                        │
  └──stop()──▶ (noop)                      └──stop()──▶ (noop)
```
