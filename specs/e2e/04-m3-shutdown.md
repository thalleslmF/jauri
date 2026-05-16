# E2E M3 — Shutdown Completo

**Marco:** M3  
**Tipo:** Teste de ciclo de vida + leak detection

## Cenário 1: Fechar janela → shutdown limpo

1. App inicia (HTTP server + GTK + WebView)
2. Fechar janela (clicar X ou `xdotool windowclose $WINDOW`)
3. Verificar:
   - `JauriApp.state === STOPPED`
   - HTTP server parou (`curl` retorna connection refused)
   - Processo Java terminou (exit code 0)
   - Nenhum socket na porta anterior (`netstat -tlnp | grep $PORT` vazio)

## Cenário 2: Chamar stop() programaticamente

1. App inicia
2. Chamar `app.stop()` de outra thread
3. Verificar mesma sequência do cenário 1

## Cenário 3: Graceful shutdown durante request

1. App inicia
2. Fazer uma requisição lenta (handler com `Thread.sleep(500)`)
3. Durante a requisição, fechar janela
4. Verificar que servidor espera a requisição terminar (timeout de 1s)
5. Nenhum socket preso após shutdown

## Cenário 4: Double shutdown não crasha

1. Chamar `app.stop()` duas vezes
2. Segunda chamada é noop (não lança exceção)
3. Estado permanece STOPPED

## Ferramentas

- `lsof -i :$PORT` pra detectar portas abertas
- `timeout` pra forçar kill após X segundos
- `strace` opcional pra detectar file descriptors vazados

## Critério de Aceite

Zero leaks. Zero processos zumbi. Shutdown é idempotente.