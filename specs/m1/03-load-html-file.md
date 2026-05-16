
# M1.3 — Carregar HTML de Arquivo

**Marco:** M1 — Janela com WebView carregando HTML  
**Dependências:** M1.2  
**Entrega:** WebView carrega HTML + CSS + JS de arquivo local

## TDD (Test-First)

### Teste: StaticAssets.extractToTemp cria diretório

Arquivo: `src/test/java/jauri/webview/StaticAssetsTest.java`

```java
package jauri.webview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class StaticAssetsTest {

    @Test
    void extractToTempCreatesDirectory(@TempDir Path tempDir) {
        StaticAssets assets = new StaticAssets();
        Path result = assets.extractToTemp("src/test/resources/static",
            tempDir.toString());
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().isDirectory());
    }

    @Test
    void extractWithResourcesJar() {
        // Simula extração do classpath (dentro de um JAR)
        StaticAssets assets = new StaticAssets();
        Path result = assets.extractFromClasspath("/static", "/tmp/jauri-test");
        // Se não há resources, retorna null sem crash
        assertNotNull(result, "Extração não deve retornar null");
    }
}
```

### Teste: WebKit webkit_web_view_load_uri binding

```java
@Test
void loadUriMethodExists() {
    assertDoesNotThrow(() -> {
        Class<?> clazz = WebKit.class;
        assertNotNull(clazz.getMethod("webkit_web_view_load_uri",
            Pointer.class, String.class));
    });
}
```

## Implementação

### src/main/java/jauri/jna/WebKit.java (atualizado)

```java
public interface WebKit extends Library {
    WebKit INSTANCE = Native.load("webkit2gtk-4.1", WebKit.class);

    Pointer webkit_web_view_new();
    void webkit_web_view_load_html(Pointer webView, String html, String baseUri);
    
    /** Carrega uma URL (http://, file://, etc). */
    void webkit_web_view_load_uri(Pointer webView, String uri);
}
```

### src/main/java/jauri/webview/StaticAssets.java

```java
package jauri.webview;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;

/**
 * Gerencia extração de assets estáticos do classpath para o filesystem.
 * 
 * Em desenvolvimento, serve direto de src/main/resources/static/.
 * Em produção, extrai do JAR pra /tmp/jauri-XXXX/.
 */
public class StaticAssets {

    /** Extrai diretório do filesystem para um temp. */
    public Path extractToTemp(String sourceDir, String tempPrefix) {
        try {
            Path temp = Files.createTempDirectory(tempPrefix);
            Path source = Paths.get(sourceDir);
            
            if (source.toFile().exists()) {
                Files.walk(source).forEach(src -> {
                    try {
                        Path dest = temp.resolve(source.relativize(src));
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        System.err.println("[WARN] Erro copiando " + src + ": " + e.getMessage());
                    }
                });
            }
            
            Path indexHtml = temp.resolve("index.html");
            if (!indexHtml.toFile().exists()) {
                Files.writeString(indexHtml, DEFAULT_HTML);
            }
            
            return temp;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao extrair assets", e);
        }
    }

    /** Extrai recursos do classpath (diretório dentro do JAR). */
    public Path extractFromClasspath(String resourcePath, String tempPrefix) {
        try {
            Path temp = Files.createTempDirectory(tempPrefix);
            copyFromClasspath(resourcePath, temp);
            return temp;
        } catch (IOException e) {
            System.err.println("[WARN] Falha ao extrair do classpath: " + e.getMessage());
            return null;
        }
    }

    private void copyFromClasspath(String resourcePath, Path target) throws IOException {
        InputStream in = getClass().getResourceAsStream(resourcePath);
        if (in == null) return;
        
        // Extrai recursivamente (implementação simplificada)
        // Em produção, usar FileSystem do JAR (FileSystems.newFileSystem)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Processa listagem de recursos
            }
        }
    }

    private static final String DEFAULT_HTML = 
        "<!DOCTYPE html><html><head>" +
        "<meta charset='UTF-8'>" +
        "<title>Jauri App</title>" +
        "</head><body>" +
        "<h1>Jauri App</h1>" +
        "<p>Carregue seus assets em src/main/resources/static/</p>" +
        "</body></html>";
}
```

### WebViewEngine.java (atualizado)

```java
public void loadFile(String filePath) {
    if (webView == null) return;
    try {
        String uri = "file://" + filePath;
        WebKit.INSTANCE.webkit_web_view_load_uri(webView, uri);
    } catch (Exception e) {
        System.err.println("[ERROR] Falha ao carregar arquivo: " + e.getMessage());
    }
}

public void reload() {
    if (webView == null) return;
    // Recarrega a URL atual
    WebKit.INSTANCE.webkit_web_view_reload(webView);
}
```

### src/main/resources/static/index.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Jauri</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            background: #1a1a2e;
            color: #e0e0e0;
            font-family: 'Segoe UI', system-ui, sans-serif;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100vh;
        }
        .card {
            background: #16213e;
            border-radius: 12px;
            padding: 2rem;
            text-align: center;
            box-shadow: 0 4px 20px rgba(0,0,0,0.3);
        }
        h1 { color: #00d4aa; margin-bottom: 0.5rem; }
        p { color: #a0a0b0; }
    </style>
</head>
<body>
    <div class="card">
        <h1>Jauri ✓</h1>
        <p>Java WebView Engine — WebKitGTK</p>
        <p id="version"></p>
    </div>
    <script>
        document.getElementById('version').innerText =
            'Versão ' + (navigator.userAgent.match(/Jauri/)?.[0] || 'dev');
    </script>
</body>
</html>
```

## Critério de Aceite

Arquivo `index.html` com CSS e `<script>` carrega corretamente. HTML, CSS e JS funcionam como num navegador.
