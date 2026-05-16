# M2.3 — Endpoint /api/invoke (POST JSON)

**Marco:** M2 — HTTP Server + IPC Bridge  
**Dependências:** M2.1, Gson no pom.xml  
**Entrega:** POST /api/invoke recebe JSON, processa, retorna JSON

## Objetivo

Criar endpoint de comunicação entre frontend e backend.

## Tarefas

1. Adicionar `com.google.code.gson:gson:2.10.1` no pom.xml
2. Criar `jauri/server/ApiInvokeHandler.java`:
   - Lê body JSON: `{"cmd": "ping", "args": {}}`
   - Processa e retorna: `{"result": {"message": "pong"}}`
   - Em caso de erro: `{"error": "mensagem"}`
3. Registrar handler no `JauriHttpServer`

## Critério de Aceite

```bash
curl -X POST -H 'Content-Type: application/json'   -d '{"cmd":"ping","args":{}}'   http://127.0.0.1:$PORT/api/invoke
# {"result":{"message":"pong"}}
```