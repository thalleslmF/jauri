# E2E M4 — Demo SQLite Funcional

**Marco:** M4  
**Tipo:** Teste funcional do app demo

## Cenário 1: App demo inicia

1. Executar `mvn exec:java -Pdemo` (ou `scripts/run-demo.sh`)
2. Verificar que janela aparece com:
   - Campo de texto com placeholder "Digite sua SQL"
   - Botão "Executar"
   - Tabela de resultados vazia inicialmente

## Cenário 2: SELECT 1

1. Digitar `SELECT 1 AS valor` no campo de texto
2. Clicar "Executar"
3. Verificar via JS:
   ```js
   document.querySelector('#result-table').rows.length === 1
   document.querySelector('#result-table tr td').innerText === '1'
   ```

## Cenário 3: SELECT de tabela real

1. App cria tabela `test(id INTEGER, name TEXT)` no startup
2. Inserir dados via SQL
3. Verificar que resultados aparecem na tabela HTML

## Cenário 4: SQL inválido

1. Digitar SQL inválido: `SELECTT 1`
2. Clicar "Executar"
3. Verificar que mensagem de erro aparece (não crasha a webview nem o backend)

## Cenário 5: Hot reload (JAURI_DEV=true)

1. Rodar com `JAURI_DEV=true`
2. Alterar `src/main/resources/static/index.html` (trocar cor de fundo)
3. Pressionar F5 na webview
4. Verificar que a mudança aparece sem rebuild

## Critério de Aceite

App demo funcional. SQL executa, resultados aparecem, erros são tratados. Hot reload funciona.