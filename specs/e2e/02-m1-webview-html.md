# E2E M1 — WebView Renderiza HTML

**Marco:** M1  
**Tipo:** Teste end-to-end com verificação visual

## Setup

Mesmo do M0 + WebKitGTK instalado.

## Cenário 1: HTML inline renderizado

1. App inicia com `loadHtml("<h1 id='title'>Jauri</h1>")`
2. Usar `webkit_web_view_run_javascript` pra executar JS e ler o DOM:
   ```js
   document.getElementById('title').innerText === 'Jauri'
   ```
3. Binding de `webkit_web_view_run_javascript(Pointer webView, String script, Pointer callback, Pointer userData)`
4. Verificar que o callback retorna o texto "Jauri"

## Cenário 2: HTML de arquivo com assets

1. Criar `static/` com `index.html` + `style.css` + `app.js`
2. Extrair pra `/tmp/jauri-XXXX/`
3. Carregar com `webkit_web_view_load_uri("file:///tmp/jauri-XXXX/index.html")`
4. Verificar que CSS foi aplicado e JS executou (via `webkit_web_view_run_javascript`)

## Cenário 3: Resize da janela

1. Obter handle da janela com `xdotool search --name Jauri`
2. Redimensionar com `xdotool windowsize $WINDOW 800 600`
3. Verificar via JS: `window.innerWidth === 800 && window.innerHeight === 600`

## Cenário 4: Screenshot comparison (opcional)

1. Capturar screenshot com `import -window $WINDOW screenshot.png`
2. Comparar com baseline usando ImageMagick `compare`

## Critério de Aceite

HTML renderiza corretamente. CSS e JS funcionam. Resize propaga pra webview.