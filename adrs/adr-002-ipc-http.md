# ADR-002: Comunicação IPC via HTTP

**Status:** Aceito  
**Data:** 2026-05-16

## Contexto

Frontend (webview) e backend (Java) precisam se comunicar. Opções: HTTP localhost, pipe nomeado, subprocess stdin/stdout, bindings JNA pra expor funções ao JS.

## Decisão

Usar **HTTP localhost** com `com.sun.net.httpserver.HttpServer` (JDK nativo).

Formato:
- Endpoint único: `POST /api/invoke`
- Body: `{"cmd": "nome", "args": {"chave": "valor"}}`
- Resposta: `{"result": {...}}` ou `{"error": "..."}`
- Porta: escolhida pelo SO (porta 0)
- Escuta só em `127.0.0.1`

## Justificativa

Zero dependências (vem no JDK 6+). Fetch padrão do JS, sem libs especiais. Testável com curl.

## Consequências

**Positivas:** Testável com curl, zero binding nativo, hot reload natural.

**Negativas:** Overhead de serialização JSON (~1ms por chamada, irrelevante pra desktop).
