# M1.4 — WebView com Resize Automático

**Marco:** M1 — Janela com WebView carregando HTML  
**Dependências:** M1.3  
**Entrega:** Redimensionar janela → conteúdo HTML se ajusta

## Objetivo

WebView ocupa 100% do espaço da janela e redimensiona junto.

## Tarefas

1. Criar `jauri/gui/Window.java` pra gerenciar a janela GTK
2. Configurar `gtk_container_add` com `GtkBox` expand
3. Conectar callback `configure-event` via `g_signal_connect` pra notificar resize
4. Testar com HTML responsivo (ex: `width: 100%; height: 100%`)

## Critério de Aceite

Arrastar borda da janela → conteúdo HTML redimensiona proporcionalmente. WebView sempre ocupa 100% do espaço disponível.

## Constantes

```java
int GTK_ORIENTATION_VERTICAL = 0;
int GTK_EXPAND = 1;
int GTK_FILL = 1;
```