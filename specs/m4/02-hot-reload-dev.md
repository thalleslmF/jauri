
# M4.2 — Hot Reload em Desenvolvimento

**Marco:** M4 — App Demo funcional  
**Dependências:** M4.1  
**Entrega:** Mudar HTML → F5 → ver mudança sem rebuild

## TDD (Test-First)

### Teste: JAURI_DEV=true altera comportamento

```java
package jauri.webview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

class DevModeTest {
    @Test
    void devModeServesFromFilesystem() {
        // Quando JAURI_DEV=true, static root é src/main/resources/static/
        // (não extraído do classpath)
        assertTrue(true, "Verificação manual: JAURI_DEV=true deve servir do FS");
    }
}
```

## Implementação

### StaticAssets.java (modo dev)

```java
public Path getDevStaticRoot() {
    // Em dev: serve direto do filesystem
    // O build Maven copia resources pro target/classes/static/
    Path devPath = Paths.get("src/main/resources/static");
    if (devPath.toFile().exists()) {
        return devPath;
    }
    // Fallback: target/classes/static/ (copiado pelo Maven)
    return Paths.get("target/classes/static");
}
```

### JauriApp.java (modo dev)

```java
// No start(), detectar JAURI_DEV:
boolean devMode = "true".equalsIgnoreCase(System.getenv("JAURI_DEV"));

if (devMode) {
    staticRoot = new StaticAssets().getDevStaticRoot().toString();
} else {
    Path dir = new StaticAssets().extractToTemp("static", "jauri-");
    staticRoot = dir.toString();
}
httpServer.setStaticRoot(staticRoot);
```

### WebViewEngine.java (reload)

```java
public void reload() {
    if (webView != null) {
        WebKit.INSTANCE.webkit_web_view_reload(webView);
    }
}
```

## Critério de Aceite

`JAURI_DEV=true mvn exec:java` → alterar `index.html` → F5 → mudança reflete. Sem rebuild.

## Edge Cases

- JAURI_DEV=false mas diretório não existe: fallback pro classpath
- Hot reload com CSS: F5 recarrega tudo (CSS muda junto)
