
# M2.4 — HandlerRegistry: Registrar Comandos

**Marco:** M2 — HTTP Server + IPC Bridge  
**Dependências:** M2.3  
**Entrega:** Apps registram handlers com nome, engine roteia

## TDD (Test-First)

### Teste: Registry

```java
package jauri.server;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HandlerRegistryTest {

    private HandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new HandlerRegistry();
        registry.register("hello", args -> {
            JsonObject result = new JsonObject();
            String name = args.has("name") ? args.get("name").getAsString() : "World";
            result.addProperty("message", "Hello, " + name + "!");
            return result;
        });
    }

    @Test
    void registeredHandlerReturnsCorrectResult() {
        JsonObject args = new JsonObject();
        args.addProperty("name", "Jauri");
        JsonObject result = registry.handle("hello", args);
        
        assertEquals("Hello, Jauri!", result.get("message").getAsString());
    }

    @Test
    void unknownCommandThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.handle("nonexistent", new JsonObject());
        });
    }

    @Test
    void multipleHandlersWork() {
        registry.register("echo", args -> args);
        registry.register("add", args -> {
            JsonObject result = new JsonObject();
            result.addProperty("sum",
                args.get("a").getAsInt() + args.get("b").getAsInt());
            return result;
        });
        
        JsonObject args = new JsonObject();
        args.addProperty("a", 3);
        args.addProperty("b", 4);
        JsonObject result = registry.handle("add", args);
        
        assertEquals(7, result.get("sum").getAsInt());
    }

    @Test
    void doubleRegisterOverwrites() {
        registry.register("hello", args -> {
            JsonObject r = new JsonObject();
            r.addProperty("message", "Overwritten!");
            return r;
        });
        
        assertEquals("Overwritten!",
            registry.handle("hello", new JsonObject())
                .get("message").getAsString());
    }
}
```

## Implementação

### src/main/java/jauri/server/HandlerRegistry.java

```java
package jauri.server;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry de comandos do IPC. Mapeia nomes de comando → handlers Java.
 * 
 * Thread-safe (ConcurrentHashMap). Pode registrar handlers de qualquer thread.
 * 
 * Uso:
 *   HandlerRegistry registry = new HandlerRegistry();
 *   registry.register("hello", args -> { ... return result; });
 *   JsonObject result = registry.handle("hello", args);
 */
public class HandlerRegistry {

    @FunctionalInterface
    public interface JauriHandler {
        JsonObject handle(JsonObject args);
    }

    private final Map<String, JauriHandler> handlers = new ConcurrentHashMap<>();

    /** Registra um handler. Se já existe, sobrescreve. */
    public void register(String command, JauriHandler handler) {
        handlers.put(command, handler);
    }

    /** Executa um handler pelo nome. Lança exceção se não encontrado. */
    public JsonObject handle(String command, JsonObject args) {
        JauriHandler handler = handlers.get(command);
        if (handler == null) {
            throw new IllegalArgumentException(
                "Handler not found: '" + command + "'. " +
                "Registered: " + handlers.keySet());
        }
        return handler.handle(args);
    }

    /** Retorna true se o comando está registrado. */
    public boolean hasCommand(String command) {
        return handlers.containsKey(command);
    }
}
```

### ApiInvokeHandler.java (atualizado — usar registry)

```java
public class ApiInvokeHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private HandlerRegistry registry;

    public ApiInvokeHandler(HandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, wrapError("Method not allowed"));
            return;
        }
        
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            
            String cmd = json.get("cmd").getAsString();
            JsonObject args = json.getAsJsonObject("args");
            
            JsonObject result = registry.handle(cmd, args);
            sendJson(exchange, 200, wrapResult(result));
            
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 404, wrapError(e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 400, wrapError(e.getMessage()));
        }
    }

    private JsonObject wrapResult(JsonObject result) {
        JsonObject w = new JsonObject();
        w.add("result", result);
        return w;
    }

    private JsonObject wrapError(String msg) {
        JsonObject w = new JsonObject();
        w.addProperty("error", msg);
        return w;
    }

    // sendJson igual ao M2.3...
}
```

## Critério de Aceite

```bash
mvn test -Dtest=HandlerRegistryTest
```

Registrar handler → chamar → resultado correto. Handler não encontrado → exceção descritiva.
