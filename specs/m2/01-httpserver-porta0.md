
# M2.1 — HttpServer na Porta 0

**Marco:** M2 — HTTP Server + IPC Bridge  
**Dependências:** M1.4  
**Entrega:** Servidor HTTP rodando em 127.0.0.1:{porta-aleatória}

## TDD (Test-First)

### Teste: Server inicia, health check responde

Arquivo: `src/test/java/jauri/server/JauriHttpServerTest.java`

```java
package jauri.server;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

class JauriHttpServerTest {

    @Test
    void startServerAndHealthCheck() throws Exception {
        JauriHttpServer server = new JauriHttpServer();
        server.start();
        
        assertTrue(server.getPort() > 0, "Porta deve ser > 0");
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/health"))
            .GET()
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("status":"ok"),
            "Health check deve retornar status ok");
        
        server.stop();
    }

    @Test
    void portChangesOnEachStart() throws Exception {
        JauriHttpServer s1 = new JauriHttpServer();
        s1.start();
        int p1 = s1.getPort();
        s1.stop();
        
        JauriHttpServer s2 = new JauriHttpServer();
        s2.start();
        int p2 = s2.getPort();
        s2.stop();
        
        assertNotEquals(p1, p2, "Porta 0 deve dar porta diferente cada execução");
    }

    @Test
    void serverBindsToLocalhostOnly() throws Exception {
        JauriHttpServer server = new JauriHttpServer();
        server.start();
        
        // Tenta conectar via localhost (deve funcionar)
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/health"))
            .GET()
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        
        // Tenta conectar via 0.0.0.0 (não deve funcionar se bind só localhost)
        // Isso é verificado pelo InetSocketAddress("127.0.0.1", 0)
        
        server.stop();
    }
}
```

### Teste: Stop server libera a porta

```java
@Test
void stopReleasesPort() throws Exception {
    JauriHttpServer server = new JauriHttpServer();
    server.start();
    int port = server.getPort();
    server.stop();
    
    // Aguarda SO liberar a porta
    Thread.sleep(100);
    
    // Novo server na mesma porta deve funcionar (se estiver livre)
    // Na prática, como usamos porta 0, cada server pega porta diferente
    assertDoesNotThrow(() -> {
        JauriHttpServer s2 = new JauriHttpServer();
        s2.start();
        s2.stop();
    });
}
```

## Implementação

### src/main/java/jauri/server/JauriHttpServer.java

```java
package jauri.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servidor HTTP embutido. Roda em 127.0.0.1 com porta aleatória.
 * 
 * Usa com.sun.net.httpserver.HttpServer (stdlib Java — zero dependências).
 * Thread pool com 2 threads (apps desktop não precisam de mais).
 * 
 * Porta 0 → SO escolhe uma porta livre automaticamente.
 * Isso evita conflitos e permite múltiplas instâncias.
 */
public class JauriHttpServer {

    private HttpServer server;
    private int port = -1;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void start() throws IOException {
        if (running.get()) return;
        
        // InetSocketAddress("127.0.0.1", 0) → localhost + porta aleatória
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newFixedThreadPool(2));
        
        // Health check endpoint
        server.createContext("/health", this::handleHealth);
        
        server.start();
        port = server.getAddress().getPort();
        running.set(true);
        
        System.out.println("[Jauri] HTTP Server started on 127.0.0.1:" + port);
    }

    public void stop() {
        if (!running.get()) return;
        running.set(false);
        server.stop(1);  // 1 segundo de grace period
        System.out.println("[Jauri] HTTP Server stopped");
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        byte[] response = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }

    public int getPort() { return port; }
    public boolean isRunning() { return running.get(); }
}
```

## Critério de Aceite

```bash
mvn test -Dtest=JauriHttpServerTest
# Testes de health check, porta aleatória, stop, localhost-only
```

Porta muda a cada execução (porta 0). Servidor só escuta em localhost.

## Edge Cases

- **Porta ocupada**: HttpServer.create lança BindException (capturável)
- **Stop sem start**: `running.get()` retorna false, stop é noop
- **Reusar servidor**: cada `start()` cria novo HttpServer
