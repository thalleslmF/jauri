# M1.1 — JNA Binding: WebKitGTK Inicialização

**Marco:** M1 — Janela com WebView carregando HTML  
**Dependências:** M0.4, `libwebkit2gtk-4.1-dev` instalado  
**Entrega:** WebView vazia dentro da janela GTK

## Objetivo

Criar binding JNA para WebKitGTK e instanciar uma WebView dentro da janela.

## Tarefas

1. Criar `jauri/jna/WebKit.java` com interface JNA:
   - `webkit_web_view_new()` → Pointer
2. Criar `jauri/webview/WebViewEngine.java`:
   - `create(Pointer parentContainer)`: cria WebView, adiciona no container GTK
3. Alterar `Main.java` pra criar `GtkBox` como container, instanciar WebView dentro

## Critério de Aceite

Janela abre com uma área branca (a webview) dentro. Não precisa carregar HTML ainda — só a webview vazia. 0 erros, 0 segfaults.