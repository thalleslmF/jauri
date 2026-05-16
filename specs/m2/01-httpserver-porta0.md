# M2.1 — HttpServer na Porta 0

**Marco:** M2 — HTTP Server + IPC Bridge  
**Dependências:** M1.4  
**Entrega:** Servidor HTTP rodando em 127.0.0.1:{porta-aleatória}

## Objetivo

Criar servidor HTTP com `com.sun.net.httpserver.HttpServer` escutando em porta aleatória.

## Tarefas

1. Criar `jauri/server/JauriHttpServer.java`:
   - `start()`: cria `HttpServer` em `InetSocketAddress("127.0.0.1", 0)`
   - `getPort()`: retorna a porta escolhida pelo SO
   - `stop()`: para o servidor
2. Adicionar endpoint `/health` que retorna `{"status":"ok"}`
3. Usar thread pool com 2 threads (apps desktop não precisam de mais)

## Critério de Aceite

```bash
curl http://127.0.0.1:$PORT/health
# {"status":"ok"}
```

Porta muda a cada execução (porta 0). Servidor só escuta em localhost.