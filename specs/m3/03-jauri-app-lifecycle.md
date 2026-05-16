# M3.3 — JauriApp: Ciclo de Vida Completo

**Marco:** M3 — Shutdown limpo + ciclo de vida  
**Dependências:** M3.2  
**Entrega:** JauriApp orquestra startup/shutdown completo

## Objetivo

JauriApp gerencia todo o ciclo de vida: HTTP server + WebView + GTK.

## Tarefas

1. `JauriApp.start()` agora:
   - Extrai `static/` do classpath
   - Inicia `JauriHttpServer`
   - Registra handlers padrão (ping)
   - Inicia `GtkMainLoop` com webview apontando pro HTTP server
2. `JauriApp.stop()`:
   - Para HTTP server
   - Encerra GTK main loop
3. Estados: CREATED → RUNNING → STOPPED
4. Garantir ordem correta: HTTP sobe antes da webview abrir

## Critério de Aceite

App inicia, janela aparece, servidor HTTP roda. Fechar janela → tudo para em ordem correta. Teste que chama start() + stop() sem GTK completa em <1s.