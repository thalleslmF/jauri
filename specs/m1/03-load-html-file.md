# M1.3 — Carregar HTML de Arquivo

**Marco:** M1 — Janela com WebView carregando HTML  
**Dependências:** M1.2  
**Entrega:** WebView carrega HTML + CSS + JS de arquivo local

## Tarefas

1. Binding de `webkit_web_view_load_uri(Pointer webView, String uri)`
2. Extrair arquivos de `src/main/resources/static/` pra diretório temp (`/tmp/jauri-XXXX/`)
3. Método `WebViewEngine.loadFile(String filePath)`
4. Teste: criar `static/index.html` com CSS e JS inclusos
5. Garantir que WebKitGTK permite acesso a `file://` URLs

## Critério de Aceite

Arquivo `index.html` com CSS e `<script>` carrega corretamente. HTML, CSS e JS funcionam como num navegador.