
# M2.2 — Servir Arquivos Estáticos

**Marco:** M2 — HTTP Server + IPC Bridge  
**Dependências:** M2.1  
**Entrega:** HTML/CSS/JS servidos pelo HTTP server

## TDD (Test-First)

### Teste: StaticFileHandler serve arquivo existente

```java
package jauri.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

class StaticFileHandlerTest {

    @TempDir
    Path tempDir;

    @Test
    void serveExistingFile() throws Exception {
        // Cria arquivo de teste
        Path indexHtml = tempDir.resolve("index.html");
        Files.writeString(indexHtml, "<h1>Test</h1>");
        
        JauriHttpServer server = new JauriHttpServer();
        server.setStaticRoot(tempDir.toString());
        server.start();
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/index.html"))
            .GET()
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, resp.statusCode());
        assertEquals("<h1>Test</h1>", resp.body());
        assertTrue(resp.headers().firstValue("Content-Type").get().contains("text/html"));
        
        server.stop();
    }

    @Test
    void notFoundReturns404() throws Exception {
        JauriHttpServer server = new JauriHttpServer();
        server.setStaticRoot(tempDir.toString());
        server.start();
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/nonexistent.html"))
            .GET()
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(404, resp.statusCode());
        
        server.stop();
    }

    @Test
    void rootServesIndexHtml() throws Exception {
        Path indexHtml = tempDir.resolve("index.html");
        Files.writeString(indexHtml, "<h1>Index</h1>");
        
        JauriHttpServer server = new JauriHttpServer();
        server.setStaticRoot(tempDir.toString());
        server.start();
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/"))
            .GET()
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Index"));
        
        server.stop();
    }

    @Test
    void contentTypeByExtension() throws Exception {
        // CSS
        Path cssFile = tempDir.resolve("style.css");
        Files.writeString(cssFile, "body { color: red; }");
        
        JauriHttpServer server = new JauriHttpServer();
        server.setStaticRoot(tempDir.toString());
        server.start();
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/style.css"))
            .GET()
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, resp.statusCode());
        assertEquals("text/css", resp.headers().firstValue("Content-Type").get());
        
        server.stop();
    }
}
```

## Implementação

### src/main/java/jauri/server/StaticFileHandler.java

```java
package jauri.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Handler que serve arquivos estáticos do filesystem.
 * 
 * Mapeia URLs como /index.html → /caminho/do/static/index.html.
 * Detecta Content-Type por extensão do arquivo.
 * Se URL for /, tenta servir index.html.
 */
public class StaticFileHandler implements HttpHandler {

    private final Path root;
    
    private static final Map<String, String> MIME_TYPES = Map.of(
        "html", "text/html",
        "css", "text/css",
        "js", "application/javascript",
        "json", "application/json",
        "png", "image/png",
        "svg", "image/svg+xml",
        "ico", "image/x-icon",
        "woff2", "font/woff2"
    );

    public StaticFileHandler(String rootPath) {
        this.root = Paths.get(rootPath);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        // Se for /, tenta index.html
        if (path.equals("/")) {
            path = "/index.html";
        }
        
        Path file = root.resolve("." + path).normalize();
        
        // Security: evitar path traversal (../etc/passwd)
        if (!file.startsWith(root)) {
            sendError(exchange, 403, "Forbidden");
            return;
        }
        
        if (!Files.exists(file) || Files.isDirectory(file)) {
            sendError(exchange, 404, "Not Found");
            return;
        }
        
        byte[] content = Files.readAllBytes(file);
        String ext = getExtension(file.getFileName().toString());
        String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);
        OutputStream os = exchange.getResponseBody();
        os.write(content);
        os.close();
    }

    private void sendError(HttpExchange exchange, int code, String msg) throws IOException {
        byte[] content = msg.getBytes();
        exchange.sendResponseHeaders(code, content.length);
        OutputStream os = exchange.getResponseBody();
        os.write(content);
        os.close();
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? "" : filename.substring(dot + 1).toLowerCase();
    }
}
```

### JauriHttpServer.java (atualizado)

```java
// Adicionar campos:
private String staticRoot;

// Método:
public void setStaticRoot(String path) {
    this.staticRoot = path;
}

// Em start(), após criar o server:
if (staticRoot != null) {
    server.createContext("/", new StaticFileHandler(staticRoot));
}
```

## Critério de Aceite

```bash
mvn test -Dtest=StaticFileHandlerTest
```

Navegador (ou curl) acessar `http://127.0.0.1:$PORT/index.html` → HTML renderizado com CSS e JS inclusos.
