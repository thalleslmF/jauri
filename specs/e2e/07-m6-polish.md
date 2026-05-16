# E2E M6 — Polimento e Resiliência

**Marco:** M6  
**Tipo:** Teste de usabilidade + edge cases

## Cenário 1: Mensagem de erro sem webkit2gtk

1. Em máquina sem `libwebkit2gtk-4.1-dev`:
   - Executar `./jauri`
2. Verificar mensagem no stderr:
   ```
   [ERROR] Jauri requires libwebkit2gtk-4.1
   Install: sudo apt install libwebkit2gtk-4.1-dev
   ```
3. Processo termina com exit code 1 (não crasha)

## Cenário 2: Erro de handler visível no frontend

1. Chamar comando inexistente via fetch
2. Verificar que o HTML mostra mensagem de erro amigável (não JSON cru)

## Cenário 3: Logs em arquivo

1. Rodar com `JAURI_LOG_DIR=/tmp/jauri-logs`
2. Verificar que `jauri.log` existe e contém:
   - Timestamps
   - Startup sequence
   - Porta escolhida
   - Shutdown sequence

## Cenário 4: Onboarding (README test)

1. Seguir os 5 passos do README.md
2. Verificar que em <10 minutos um app "Hello World" funcional é criado
3. App mostra "Hello from Jauri!" na webview

## Critério de Aceite

Erros são claros. Logs são úteis. Qualquer dev reproduz o onboarding.