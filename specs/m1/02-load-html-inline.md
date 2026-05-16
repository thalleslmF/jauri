# M1.2 — Carregar HTML Inline na WebView

**Marco:** M1 — Janela com WebView carregando HTML  
**Dependências:** M1.1  
**Entrega:** HTML renderizado na janela

## Objetivo

Carregar HTML diretamente via string na WebView.

## Tarefas

1. Binding de `webkit_web_view_load_html(Pointer webView, String html, String baseUri)`
2. Método `WebViewEngine.loadHtml(String html)`
3. Teste: carregar `<h1>Jauri</h1><p>Funcionou!</p>`

## Critério de Aceite

`loadHtml("<h1>Jauri</h1>")` renderiza "Jauri" como heading na janela. CSS inline funciona.