# Jauri

Java WebView engine — concorrente de JCEF e JavaFX WebView. Leve, nativo, só Linux.

> Status: Spec phase. Ver [SPEC.html](SPEC.html) para roadmap completo.

## Arquitetura

```
WebView (WebKitGTK) ← HTTP localhost → Java Backend (GraalVM)
```

## Documentação

- [SPEC.html](SPEC.html) — Roadmap M0-M6 com tarefas e critérios de aceite
- [plans/adr-001-architecture-overview.html](plans/adr-001-architecture-overview.html) — Arquitetura geral
- [plans/adr-002-ipc-http.html](plans/adr-002-ipc-http.html) — IPC via HTTP
- [plans/adr-003-embedding-jna.html](plans/adr-003-embedding-jna.html) — Embedding via JNA
- [plans/adr-004-serialization-gson.html](plans/adr-004-serialization-gson.html) — Serialização Gson
- [plans/adr-005-frontend-html.html](plans/adr-005-frontend-html.html) — Frontend HTML puro

## Licença

MIT
