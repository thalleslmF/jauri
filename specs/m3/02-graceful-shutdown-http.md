# M3.2 — Graceful Shutdown do HTTP Server

**Marco:** M3 — Shutdown limpo + ciclo de vida  
**Dependências:** M2.1, M3.1  
**Entrega:** Server fecha sem sockets presos

## Objetivo

HTTP server para corretamente, esperando requisições em andamento.

## Tarefas

1. Em `JauriHttpServer.stop()`:
   - `HttpServer.stop(1)`: para com 1 segundo de grace period
   - Esperar executor thread pool terminar
   - Marcar estado como STOPPED
2. Garantir que `stop()` é thread-safe (pode ser chamado de qualquer thread)
3. Testar com requisição lenta simulada (sleep de 500ms)

## Critério de Aceite

Matar app durante requisição lenta → servidor fecha sem socket preso. `netstat -tlnp | grep $PORT` não mostra nada após stop().