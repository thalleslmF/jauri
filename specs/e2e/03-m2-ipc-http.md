# E2E M2 — IPC HTTP Completo

**Marco:** M2  
**Tipo:** Teste integrado HTTP + WebView

## Cenário 1: HTTP server saudável

```bash
# Server deve responder health check
curl http://127.0.0.1:$PORT/health
# {"status":"ok"}
```

## Cenário 2: API invoke com JSON

```bash
curl -X POST -H 'Content-Type: application/json' \
  -d '{"cmd":"ping","args":{}}' \
  http://127.0.0.1:$PORT/api/invoke
# {"result":{"message":"pong"}}
```

## Cenário 3: Handler não encontrado

```bash
curl -X POST -H 'Content-Type: application/json' \
  -d '{"cmd":"nao_existe","args":{}}' \
  http://127.0.0.1:$PORT/api/invoke
# {"error":"Handler not found: nao_existe"}
```

## Cenário 4: HTML → JS → fetch → backend → resultado

1. App inicia com `index.html` que tem um botão "Ping"
2. Usar `webkit_web_view_run_javascript` pra:
   ```js
   fetch('/api/invoke', {
     method: 'POST',
     body: JSON.stringify({cmd:'ping', args:{}})
   }).then(r => r.json()).then(d => {
     window.__lastResult = d.result.message;
   })
   ```
3. Aguardar 500ms (setTimeout no JS)
4. Verificar: `window.__lastResult === 'pong'`

## Critério de Aceite

Frontend e backend se comunicam via HTTP. Erros são propagados corretamente.