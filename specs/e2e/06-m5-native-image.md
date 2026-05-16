# E2E M5 — Native Image Build + Run

**Marco:** M5  
**Tipo:** Teste de build + binário

## Cenário 1: Build bem-sucedido

1. `mvn package`
2. `native-image -jar target/jauri.jar jauri [flags]`
3. Verificar:
   - Exit code 0
   - Binário `jauri` existe
   - Nenhum warning JNA no build log
   - Tamanho < 20MB

## Cenário 2: Demo roda como native-image

1. Executar `./jauri`
2. Verificar que janela abre com app demo
3. Executar SELECT 1
4. Fechar janela

## Cenário 3: Release zip

1. Executar `make release`
2. Verificar que `jauri-linux-x64.zip` existe
3. Extrair e verificar conteudo:
   - `jauri` (binário)
   - `README.md`
   - `INSTALL.md`

## Cenário 4: Rodar sem JDK

1. Em máquina limpa (sem JDK, só libs sistema):
   - Copiar `jauri-linux-x64.zip`
   - Extrair
   - Executar `./jauri`
2. Verificar que app abre sem erro de class not found ou JVM

## Critério de Aceite

Binário < 20MB. Demo roda sem JDK. Release zip é autocontido.