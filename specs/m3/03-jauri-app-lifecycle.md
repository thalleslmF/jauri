
# M3.3 — JauriApp: Ciclo de Vida Completo

**Marco:** M3 — Shutdown limpo + ciclo de vida  
**Dependências:** M3.2  
**Entrega:** JauriApp orquestra startup/shutdown completo

## TDD (Test-First)

### Teste: Ciclo de vida completo orquestrado

```java
package jauri;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "DISPLAY", matches = ".*")
class JauriAppFullLifecycleTest {

    @Test
    void startAndStopOrchestratesAllComponents() throws Exception {
        JauriApp app = new JauriApp();
        
        // start() deve iniciar HTTP primeiro, depois GTK
        app.start();
        
        // Verificar HTTP rodando
        assertTrue(app.getHttpPort() > 0);
        assertEquals(JauriApp.State.RUNNING, app.getState());
        
        // stop() deve parar HTTP primeiro, depois GTK
        app.stop();
        app.await();
        
        assertEquals(JauriApp.State.STOPPED, app.getState());
    }

    @Test
    void appStartupOrderCorrect() {
        // HTTP sobe antes da webview abrir
        // GTK é iniciado por último
        JauriApp app = new JauriApp();
        app.start();
        
        // HTTP deve estar rodando
        assertTrue(app.isHttpRunning());
        
        app.stop();
        app.await();
    }
}
```

## Implementação

### JauriApp.java (final — versão completa)

```java
package jauri;

import jauri.gui.GtkMainLoop;
import jauri.gui.Window;
import jauri.server.*;
import jauri.webview.StaticAssets;
import jauri.webview.WebViewEngine;
import com.google.gson.JsonObject;
import java.nio.file.Path;

public class JauriApp {

    public enum State { CREATED, RUNNING, STOPPED }

    private volatile State state = State.CREATED;
    private JauriHttpServer httpServer;
    private GtkMainLoop gtkLoop;
    private HandlerRegistry registry;

    public JauriApp start() {
        if (state != State.CREATED) return this;
        state = State.RUNNING;
        
        try {
            // 1. Registry (antes do HTTP — handlers precisam estar prontos)
            registry = new HandlerRegistry();
            registerDefaultHandlers();
            
            // 2. HTTP server (sobe antes da webview)
            httpServer = new JauriHttpServer();
            Path staticDir = new StaticAssets()
                .extractToTemp("src/main/resources/static", "jauri-");
            httpServer.setStaticRoot(staticDir.toString());
            httpServer.setRegistry(registry);
            httpServer.start();
            
            int port = httpServer.getPort();
            
            // 3. GTK + WebView (último — depende do HTTP estar pronto)
            String url = "http://127.0.0.1:" + port + "/index.html";
            gtkLoop = new GtkMainLoop(url);
            gtkLoop.setOnClose(this::stop);
            gtkLoop.start();
            
            System.out.println("[Jauri] ✓ App running → " + url);
            
        } catch (Exception e) {
            System.err.println("[Jauri] ✗ Failed to start: " + e.getMessage());
            e.printStackTrace();
            stop();
        }
        
        return this;
    }

    public JauriApp stop() {
        if (state != State.RUNNING) return this;
        state = State.STOPPED;
        
        // Ordem inversa: GTK primeiro (para de responder) → HTTP (libera porta)
        if (gtkLoop != null) gtkLoop.stop();
        if (httpServer != null) httpServer.stop();
        
        System.out.println("[Jauri] ✓ App stopped gracefully");
        return this;
    }

    public void await() {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getHttpPort() { 
        return httpServer != null ? httpServer.getPort() : -1; 
    }

    public boolean isHttpRunning() {
        return httpServer != null && httpServer.isRunning();
    }

    public State getState() { return state; }

    private void registerDefaultHandlers() {
        registry.register("ping", args -> {
            JsonObject r = new JsonObject();
            r.addProperty("message", "pong");
            return r;
        });
        registry.register("app.info", args -> {
            JsonObject r = new JsonObject();
            r.addProperty("name", "Jauri");
            r.addProperty("version", "0.1.0");
            r.addProperty("port", httpServer != null ? httpServer.getPort() : -1);
            return r;
        });
    }
}
```

## Critério de Aceite

App inicia (HTTP → WebView), janela aparece, servidor HTTP roda. Fechar janela → tudo para em ordem inversa (GTK → HTTP). `start()` + `stop()` sem GTK completa.
