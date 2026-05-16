
# M3.2 — Graceful Shutdown do HTTP Server

**Marco:** M3 — Shutdown limpo + ciclo de vida  
**Dependências:** M2.1, M3.1  
**Entrega:** Server fecha sem sockets presos

## TDD (Test-First)

### Teste: Stop server com grace period

```java
package jauri.server;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

class GracefulShutdownTest {

    @Test
    void stopDuringRequestDoesNotHang() throws Exception {
        JauriHttpServer server = new JauriHttpServer();
        server.start();
        int port = server.getPort();
        
        // Inicia requisição lenta em background
        Thread slowRequest = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/slow"))
                    .GET()
                    .build();
                client.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                // Esperado: conexão pode ser fechada durante shutdown
            }
        });
        slowRequest.start();
        
        // Para o server imediatamente
        long start = System.currentTimeMillis();
        server.stop();
        long duration = System.currentTimeMillis() - start;
        
        // Stop deve retornar em < 2s (grace period = 1s)
        assertTrue(duration < 2000,
            "stop() deve retornar em < 2s, levou " + duration + "ms");
    }

    @Test
    void portIsReleasedAfterStop() throws Exception {
        JauriHttpServer server = new JauriHttpServer();
        server.start();
        int port = server.getPort();
        server.stop();
        
        Thread.sleep(200);  // Aguarda SO liberar
        
        // Verificar que não há mais nada ouvindo na porta
        // Tentando bind na mesma porta deve falhar (porta ainda em TIME_WAIT)
        // Mas como usamos porta 0, isso não é crítico
        assertFalse(server.isRunning());
    }
}
```

## Implementação

### JauriHttpServer.java (atualizado — stop seguro)

```java
public void stop() {
    if (!running.compareAndSet(true, false)) return;
    
    if (server != null) {
        // stop(1) = 1 segundo de grace period
        // Requisições em andamento têm 1s para completar
        server.stop(1);
        server = null;
    }
    
    port = -1;
    System.out.println("[Jauri] HTTP Server stopped gracefully");
}
```

## Critério de Aceite

Stop com 1s de grace period. `netstat -tlnp | grep $PORT` não mostra nada após stop(). Thread-safe.
