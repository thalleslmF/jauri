# M5.3 — Reduzir Tamanho do Binário

**Marco:** M5 — GraalVM Native Image  
**Dependências:** M5.2  
**Entrega:** Binário < 20MB

## Objetivo

Analisar e reduzir o tamanho do binário compilado.

## Tarefas

1. Rodar `native-image --verbose` e analisar o que mais pesa
2. Usar `-H:+ReportUnusedElements` pra detectar código não usado
3. Remover dependências não essenciais do pom.xml
4. Testar com `-H:+PrintImageObjectTree` pra ver a árvore de objetos
5. Alvo: < 20MB (benchmark: Tauri Rust hello world ~3MB, Electron ~150MB)

## Critério de Aceite

`ls -lh jauri` mostra < 20MB. App demo SQLite roda normalmente.