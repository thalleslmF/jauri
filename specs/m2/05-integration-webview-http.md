# M2.5 — Integração: WebView + HTTP Server

**Marco:** M2 — HTTP Server + IPC Bridge  
**Dependências:** M2.4, M1.4  
**Entrega:** WebView carrega HTML do HTTP server, JS chama backend

## Objetivo

Juntar WebView e HTTP Server: ao iniciar, servir HTML do HTTP e abrir webview apontando pra lá.

## Tarefas

1. Em `JauriApp.start()`:
   - Extrair `static/` do classpath pra `/tmp/jauri-XXXX/`
   - Iniciar `JauriHttpServer` servindo desse diretório
   - Abrir webView em `http://127.0.0.1:$PORT/index.html`
2. Criar `static/index.html` de exemplo com:
   - Botão que chama `/api/invoke` com `cmd: "ping"`
   - Exibe resultado na tela
3. Registrar handler `ping` no startup

## Critério de Aceite

App inicia → janela aparece com HTML renderizado → clicar botão "Ping" → resultado "pong" aparece na tela. Tudo via fetch HTTP local.