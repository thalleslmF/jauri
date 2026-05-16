# M5.4 — Script de Release

**Marco:** M5 — GraalVM Native Image  
**Dependências:** M5.3  
**Entrega:** `make release` gera .zip pronto pra distribuir

## Objetivo

Automatizar o processo de build + release.

## Tarefas

1. Criar `scripts/release.sh`:
   - Build Maven (`mvn package`)
   - Copiar demo app JAR
   - Rodar native-image
   - Compactar em `jauri-linux-x64.zip` com binário + README
2. Adicionar target `release` no Makefile
3. Escrever `INSTALL.md` com instruções de instalação das dependências

## Critério de Aceite

Rodar `make release` → `jauri-linux-x64.zip` contém binário + README + INSTALL.md. Tamanho < 10MB comprimido.