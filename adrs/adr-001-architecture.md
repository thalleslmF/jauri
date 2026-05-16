# ADR-001: Arquitetura em 4 Camadas

**Status:** Aceito  
**Data:** 2026-05-16

## Contexto

Jauri precisa executar apps desktop Java usando webview nativa. Precisamos de uma arquitetura que isole responsabilidades e seja substituível em cada camada.

## Decisão

Arquitetura em 4 camadas com dependência unidirecional:

```
Layer 3: Runtime (orquestra startup/shutdown)
Layer 2: IPC Bridge (HTTP localhost, POST /api/invoke)
Layer 1: WebView Engine (JNA → GTK3 + WebKitGTK 4.1)
Layer 0: Native Platform (libgtk-3.so, libwebkit2gtk-4.1.so)
```

Regras:
- Layer N só conhece Layer N-1
- Layer 1 é a única que sabe de JNA/GTK
- Layer 2 e 3 são testáveis sem GTK (mock)
- HTTP server sempre em porta 0 (SO escolhe)

## Consequências

**Positivas:** Cada camada ~100-300 linhas. Se GTK crashar, erro tá numa camada específica.

**Negativas:** JNA binding frágil. Thread GTK vs thread HTTP precisam sincronizar no shutdown.
