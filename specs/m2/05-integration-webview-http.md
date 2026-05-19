
# M2.5 — Integração: WebView + HTTP Server

**Marco:** M2 — HTTP Server + IPC Bridge  
**Dependências:** M2.4, M1.4  
**Entrega:** WebView carrega HTML do HTTP server, JS chama backend via fetch

## TDD (Test-First)

### Teste: JauriApp.start() com HTTP + WebView

```java
package jauri;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "DISPLAY", matches = ".*")
class JauriAppHttpIntegrationTest {

    @Test
    void startServesStaticAndOpensWebView() throws Exception {
        JauriApp app = new JauriApp();
        app.start();
        
        // HTTP deve estar rodando
        assertTrue(app.getHttpPort() > 0);
        
        // index.html deve ser servido
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + app.getHttpPort() + "/index.html"))
            .GET()
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Jauri"),
            "index.html deve conter 'Jauri': " + resp.body().substring(0, Math.min(100, resp.body().length())));
        
        app.stop();
        app.await();
    }

    @Test
    void pingViaApiInvoke() throws Exception {
        JauriApp app = new JauriApp();
        app.start();
        
        HttpClient client = HttpClient.newHttpClient();
        String body = "{\"cmd\":\"ping\",\"args\":{}}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + app.getHttpPort() + "/api/invoke"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("pong"));
        
        app.stop();
        app.await();
    }
}
```

## Implementação

### src/main/resources/static/index.html (com fetch para backend)

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Jauri Demo</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            background: #1a1a2e;
            color: #e0e0e0;
            font-family: 'Segoe UI', system-ui, sans-serif;
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
        }
        .card {
            background: #16213e;
            border-radius: 12px;
            padding: 2rem;
            text-align: center;
            box-shadow: 0 4px 20px rgba(0,0,0,0.3);
            max-width: 400px;
        }
        h1 { color: #00d4aa; margin-bottom: 0.5rem; }
        p { color: #a0a0b0; margin-bottom: 1rem; }
        button {
            background: #00d4aa;
            border: none;
            color: #1a1a2e;
            padding: 12px 24px;
            border-radius: 8px;
            font-size: 16px;
            font-weight: bold;
            cursor: pointer;
            transition: transform 0.2s, background 0.2s;
        }
        button:hover { background: #00e6b8; transform: scale(1.05); }
        button:active { transform: scale(0.95); }
        #result {
            margin-top: 1rem;
            padding: 1rem;
            background: #0f3460;
            border-radius: 8px;
            font-family: monospace;
            min-height: 40px;
            word-break: break-all;
        }
        .error { color: #e74c3c; }
        .success { color: #2ecc71; }
    </style>
</head>
<body>
    <div class="card">
        <h1>Jauri Demo</h1>
        <p>Java WebView Engine — IPC via HTTP</p>
        <button id="pingBtn">Ping Backend</button>
        <div id="result">Clique no botao para testar</div>
    </div>
    <script>
        async function invoke(cmd, args = {}) {
            const resp = await fetch('/api/invoke', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ cmd, args })
            });
            return resp.json();
        }

        document.getElementById('pingBtn').addEventListener('click', async () => {
            const resultDiv = document.getElementById('result');
            resultDiv.textContent = 'Chamando backend...';
            resultDiv.className = '';
            
            try {
                const data = await invoke('ping');
                if (data.error) {
                    resultDiv.textContent = 'Erro: ' + data.error;
                    resultDiv.className = 'error';
                } else {
                    resultDiv.textContent = JSON.stringify(data.result, null, 2);
                    resultDiv.className = 'success';
                }
            } catch (err) {
                resultDiv.textContent = 'Erro de conexao: ' + err.message;
                resultDiv.className = 'error';
            }
        });
    </script>
</body>
</html>
```

### JauriApp.java (atualizado — start completo)

```java
package jauri;

import jauri.gui.GtkMainLoop;
import jauri.gui.Window;
import jauri.server.*;
import jauri.webview.StaticAssets;
import jauri.webview.WebViewEngine;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class JauriApp {

    public enum State { CREATED, RUNNING, STOPPED }

    private final AtomicReference<State> state = new AtomicReference<>(State.CREATED);
    private final CountDownLatch stoppedLatch = new CountDownLatch(1);
    private GtkMainLoop gtkLoop;
    private JauriHttpServer httpServer;
    private HandlerRegistry registry;
    private WebViewEngine webView;

    public JauriApp start() {
        if (!state.compareAndSet(State.CREATED, State.RUNNING)) {
            return this;
        }
        
        try {
            // 1. Handler registry (antes do HTTP)
            registry = new HandlerRegistry();
            registry.register("ping", args -> {
                JsonObject r = new JsonObject();
                r.addProperty("message", "pong");
                return r;
            });
            
            // 2. HTTP server
            httpServer = new JauriHttpServer();
            
            // Extrai assets estáticos
            StaticAssets assets = new StaticAssets();
            Path staticDir = assets.extractToTemp(
                "src/main/resources/static", "jauri-");
            httpServer.setStaticRoot(staticDir.toString());
            
            // Adiciona endpoint /api/invoke com registry
            httpServer.setRegistry(registry);
            
            httpServer.start();
            int port = httpServer.getPort();
            
            // 3. GTK + WebView apontando pro HTTP server
            gtkLoop = new GtkMainLoop();
            webView = new WebViewEngine();
            gtkLoop.start(webView, port);  // GtkMainLoop agora recebe a WebView + porta
            
            System.out.println("[Jauri] App started → http://127.0.0.1:" + port);
            
        } catch (Exception e) {
            System.err.println("[Jauri] Failed to start: " + e.getMessage());
            e.printStackTrace();
            stop();
        }
        
        return this;
    }

    public JauriApp stop() {
        State prev = state.getAndUpdate(s -> 
            s == State.RUNNING ? State.STOPPED : s
        );
        
        if (prev == State.RUNNING) {
            if (httpServer != null) httpServer.stop();
            if (gtkLoop != null) gtkLoop.stop();
            stoppedLatch.countDown();
        }
        
        return this;
    }

    public void await() {
        try {
            stoppedLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getHttpPort() {
        return httpServer != null ? httpServer.getPort() : -1;
    }

    public State getState() { return state.get(); }
}
```

### JauriHttpServer.java (atualizado — setRegistry)

```java
private HandlerRegistry registry;

public void setRegistry(HandlerRegistry reg) {
    this.registry = reg;
}

// Em start():
server.createContext("/api/invoke", new ApiInvokeHandler(registry));
```

### GtkMainLoop.java (atualizado — recebe WebView + porta)

```java
public void start(WebViewEngine engine, int httpPort) {
    // ... existing init ...
    Pointer box = gtk.gtk_box_new(0, 0);
    gtk.gtk_container_add(window, box);
    
    Pointer wv = engine.create(box);
    engine.loadFile("http://127.0.0.1:" + httpPort + "/index.html");
    
    gtk.gtk_widget_show_all(window);
    running.set(true);
    gtk.gtk_main();
}
```

### Main.java (atualizado — demo funcional)

```java
package jauri;

public class Main {
    public static void main(String[] args) {
        new JauriApp()
            .start()
            .await();
    }
}
```

## Critério de Aceite

`mvn exec:java` → janela aparece com HTML renderizado → clicar botão "Ping Backend" → resultado "pong" aparece na tela. Tudo via fetch HTTP local.
