
# M2.3 — Endpoint /api/invoke (POST JSON)

**Marco:** M2 — HTTP Server + IPC Bridge  
**Dependências:** M2.1, Gson no pom.xml  
**Entrega:** POST /api/invoke recebe JSON, processa, retorna JSON

## TDD (Test-First)

### Teste: Ping command funciona

```java
package jauri.server;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiInvokeHandlerTest {

    @Test
    void pingReturnsPong() {
        ApiInvokeHandler handler = new ApiInvokeHandler(null);  // null registry por enquanto
        
        JsonObject request = new JsonObject();
        request.addProperty("cmd", "ping");
        request.add("args", new JsonObject());
        
        JsonObject response = handler.handlePing(request);
        
        assertEquals("pong", response.get("message").getAsString());
    }
}
```

### Teste: Integração HTTP

```java
@Test
void pingViaHttp() throws Exception {
    JauriHttpServer server = new JauriHttpServer();
    server.start();
    
    HttpClient client = HttpClient.newHttpClient();
    String body = "{\"cmd\":\"ping\",\"args\":{}}";
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/api/invoke"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
    
    assertEquals(200, resp.statusCode());
    assertTrue(resp.body().contains("pong"),
        "Resposta deve conter pong: " + resp.body());
    
    server.stop();
}
```

## Implementação

### pom.xml (adicionar Gson)

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

### src/main/java/jauri/server/ApiInvokeHandler.java

```java
package jauri.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handler para POST /api/invoke.
 * 
 * Frontend HTML envia: {\"cmd\": "ping", \"args\": {}}
 * Backend responde:    {\"result\": {\"message\": "pong"}}
 * Em erro:             {\"error\": "mensagem"}
 */
public class ApiInvokeHandler implements HttpHandler {

    private final Gson gson = new Gson();
    private HandlerRegistry registry;

    public ApiInvokeHandler(HandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, errorJson("Method not allowed"));
            return;
        }
        
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            
            String cmd = json.get("cmd").getAsString();
            JsonObject args = json.getAsJsonObject("args");
            
            JsonObject response;
            if (registry != null) {
                response = registry.handle(cmd, args);
            } else if ("ping".equals(cmd)) {
                response = handlePing(args);
            } else {
                response = errorJson("Unknown command: " + cmd);
            }
            
            JsonObject wrapper = new JsonObject();
            wrapper.add("result", response);
            sendJson(exchange, 200, wrapper);
            
        } catch (Exception e) {
            sendJson(exchange, 400, errorJson(e.getMessage()));
        }
    }

    public JsonObject handlePing(JsonObject args) {
        JsonObject result = new JsonObject();
        result.addProperty("message", "pong");
        return result;
    }

    private void sendJson(HttpExchange exchange, int status, JsonObject json)
            throws IOException {
        byte[] content = gson.toJson(json).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, content.length);
        OutputStream os = exchange.getResponseBody();
        os.write(content);
        os.close();
    }

    private JsonObject errorJson(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        return error;
    }
}
```

## Critério de Aceite

```bash
curl -X POST -H 'Content-Type: application/json'   -d '{"cmd":"ping","args":{}}'   http://127.0.0.1:$PORT/api/invoke
# {"result":{"message":"pong"}}
```

Testes JUnit validam parse do JSON, roteamento e resposta de erro.
