# M4.2 — Hot Reload em Desenvolvimento

**Marco:** M4 — App Demo funcional  
**Dependências:** M4.1  
**Entrega:** Mudar HTML → recarregar webview → ver mudança

## Objetivo

Durante desenvolvimento, servir HTML direto do filesystem sem rebuildar o JAR.

## Tarefas

1. Se `JAURI_DEV=true`:
   - Servir HTML de `src/main/resources/static/` (não extrair do classpath)
   - Adicionar shortcut F5 pra recarregar webview
2. `WebViewEngine.reload()`: recarrega a URL atual
3. Binding de callback de teclado na webview pra detectar F5

## Critério de Aceite

`JAURI_DEV=true mvn exec:java` → alterar `index.html` → F5 → mudança reflete. Sem rebuild, sem restart.