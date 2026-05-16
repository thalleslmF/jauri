# ADR-005: Frontend HTML Puro

**Status:** Aceito  
**Data:** 2026-05-16

## Contexto

Qual tecnologia usar pra UI dos apps Jauri.

## Decisão

Jauri engine **não impõe framework frontend**. Serve arquivos HTML/CSS/JS estáticos. Cada app escolhe: vanilla, React, Svelte, Alpine.js, htmx, etc.

Contrato engine-app:
1. App coloca assets em `src/main/resources/static/`
2. Engine extrai pra `/tmp/jauri-XXXX/` e serve via HTTP
3. JS faz `fetch('/api/invoke', {method:'POST', body: JSON.stringify({cmd, args})})`
4. Engine retorna `{"result": ...}` ou `{"error": "..."}`

## Justificativa

Escolha do framework é do app, não da engine. HTML/CSS/JS é universal. Separação clara.

## Consequências

**Positivas:** Flexibilidade total, ecossistema web completo (CodeMirror, DataGrid, Chart.js).

**Negativas:** App precisa saber HTML/CSS/JS mínimo.
