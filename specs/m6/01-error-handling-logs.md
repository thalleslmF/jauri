# M6.1 — Tratamento de Erros + Logs

**Marco:** M6 — Polimento + distribuicao  
**Dependencias:** M5  
**Entrega:** Erros claros e logs uteis

## Implementacao

### src/main/java/jauri/util/JauriLogger.java

```java
package jauri.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger simples do Jauri (sem dependencias externas).
 * 
 * Niveis: INFO, WARN, ERROR
 * Formato: [JAURI] HH:MM:SS [LEVEL] mensagem
 */
public class JauriLogger {

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void info(String msg) {
        log("INFO", msg);
    }

    public static void warn(String msg) {
        log("WARN", msg);
    }

    public static void error(String msg) {
        log("ERROR", msg);
    }

    public static void error(String msg, Throwable t) {
        log("ERROR", msg);
        t.printStackTrace(System.err);
    }

    private static void log(String level, String msg) {
        System.out.printf("[JAURI] %s [%s] %s%n",
            LocalTime.now().format(TIME_FMT), level, msg);
    }
}
```

### Startup com verificacao de dependencias

```java
// Em JauriApp.start(), verificar dependencias de sistema:
private void checkSystemDependencies() {
    try {
        Gtk3.INSTANCE.gtk_init(0, null);
    } catch (UnsatisfiedLinkError e) {
        System.err.println("""
            [JAURI] ERROR: GTK3 nao encontrado!
            Instale: sudo apt install libgtk-3-dev
            """);
        throw new RuntimeException("GTK3 required", e);
    }

    try {
        WebKit.INSTANCE.webkit_web_view_new();
    } catch (UnsatisfiedLinkError e) {
        System.err.println("""
            [JAURI] ERROR: WebKitGTK nao encontrado!
            Instale: sudo apt install libwebkit2gtk-4.1-dev
            """);
        throw new RuntimeException("WebKitGTK required", e);
    }
}
```

### Mensagens de erro descritivas

- **GTK3 nao instalado**: exibe comando apt exato pra copiar/colar
- **WebKitGTK nao instalado**: exibe comando apt exato
- **Display nao disponivel**: sugere xvfb-run ou export DISPLAY
- **Porta ocupada**: exibe qual processo esta usando (lsof/sock)
- **JNA UnsatisfiedLinkError**: captura com try/catch e exibe msg clara

## Criterio de Aceite

Rodar sem webkit2gtk: mensagem clara de erro apontando o comando `sudo apt install libwebkit2gtk-4.1-dev`. Rodar normal: logs limpos no formato padrao. Stdout sem poluicao desnecessaria.
