# M2.2 — Servir Arquivos Estáticos

**Marco:** M2 — HTTP Server + IPC Bridge  
**Dependências:** M2.1  
**Entrega:** HTML/CSS/JS servidos pelo HTTP server

## Objetivo

Servir arquivos estáticos do diretório `static/` via HTTP.

## Tarefas

1. Criar `jauri/server/StaticFileHandler.java`:
   - Mapear `/` → diretório de assets
   - Detectar Content-Type por extensão (.html, .css, .js, .png, .svg)
2. Alterar `JauriHttpServer` pra servir de:
   - Durante dev: diretório `src/main/resources/static/`
   - Em produção: diretório temp extraído do JAR
3. Testar: `curl http://127.0.0.1:$PORT/index.html` retorna o HTML correto

## Critério de Aceite

Navegador (ou curl) acessar `http://127.0.0.1:$PORT/index.html` → HTML renderizado com CSS e JS inclusos.