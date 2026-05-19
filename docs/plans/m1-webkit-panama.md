# M1 — WebKit WebView com Panama FFM — Implementation Plan
> **Status:** ✅ Implementado (PR #13)

> **Stack:** Java 24 GraalVM CE + Panama FFM (JEP 454) + GTK 3.24 + WebKitGTK 4.1.2 + JUnit 5
> **Ref:** ADR-003 (Panama FFM), M0 pattern (Gtk3.java), `libwebkit2gtk-4.1.so`

**Goal:** Binário Java que abre janela GTK com WebKit WebView carregando HTML inline.

**Architecture:** Bindings WebKit via Panama FFM (`java.lang.foreign.Linker` + `SymbolLookup`) em `libwebkit2gtk-4.1.so`. Mesmo padrão do M0: static init com `STUB_ARENA`, `downcallHandle`, `FunctionDescriptor`. WebView é `GtkWidget*` — adicionado à janela com `gtk_container_add`.

**Dependências:** M0 completo (PR #12 já mergeado) — `Gtk3.java`, `GtkMainLoop.java`.

**Package:** `jauri.ffm` (mesmo pacote).

---

### Task 1: M1.1 — gtk_container_add + WebKitWebView.new

**Arquivos:**
- `src/main/java/jauri/ffm/Gtk3.java` — adicionar binding `gtk_container_add`
- `src/main/java/jauri/ffm/WebKit.java` — novo binding para `libwebkit2gtk-4.1.so`
- `src/main/java/jauri/ffm/JauriWebView.java` — classe wrapper
- `src/test/java/jauri/ffm/WebKitTest.java` — verifica static init
- `src/main/java/jauri/Main.java` — atualizar para criar janela + webview

**Bindings WebKit:**
```
webkit_web_view_new() — ()long
```

**Gtk3 additions:**
```
gtk_container_add(GtkContainer*, GtkWidget*) — (long, long)void
```

**Main.java alteração:** Após criar janela, criar WebView com `WebKit.webkitWebViewNew()`, adicionar com `Gtk3.gtkContainerAdd(window, webview)`, mostrar tudo.

**Critério:** `mvn compile exec:java` abre janela com área vazia (WebView) dentro. 0 erros.

### Task 2: M1.2 — webkit_web_view_load_html + JauriWebView.loadHtml()

**Arquivos:**
- `src/main/java/jauri/ffm/WebKit.java` — adicionar `webkit_web_view_load_html`
- `src/main/java/jauri/ffm/JauriWebView.java` — método `loadHtml(String html, String baseUri)`
- `src/test/java/jauri/M1EndToEndTest.java` — test E2E com HTML inline (requer xvfb)

**Binding:**
```
webkit_web_view_load_html(WebKitWebView*, const gchar* content, const gchar* base_uri) — (long, long, long)void
```

**Panama FFM details:**
- `FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_LONG)`
- String → `arena.allocateFrom(text)` via `java.lang.foreign.Linker` UTF-8 encoding
- `base_uri` pode ser `NULL` (passar `0L`)

**Critério:** `loadHtml("<h1>Jauri</h1>", null)` renderiza "Jauri" na janela.

### Task 3: M1.3 — webkit_web_view_load_uri + loadUri()

**Arquivos:**
- `src/main/java/jauri/ffm/WebKit.java` — adicionar `webkit_web_view_load_uri`
- `src/main/java/jauri/ffm/JauriWebView.java` — método `loadUri(String uri)`

**Binding:**
```
webkit_web_view_load_uri(WebKitWebView*, const gchar* uri) — (long, long)void
```

**Critério:** `loadUri("file:///tmp/jauri-test/index.html")` carrega HTML com CSS e JS.

### Task 4: M1.4 — WebView resize automático

**Arquivos:**
- `JauriWebView.java` — nada adicional (já é GtkWidget, expand via container já feito)
- `Main.java` — configurar tamanho mínimo da janela

**WebView resize:** WebKitWebView estende GtkWidget, que estende GInitiallyUnowned. Quando adicionado com `gtk_container_add` dentro de uma GtkWindow com `GTK_WIN_POS_CENTER`, o GTK expande automaticamente. Para garantir: `gtk_widget_set_hexpand` e `gtk_widget_set_vexpand` no WebView.

**Bindings GTK adicionais (em Gtk3.java):**
```
gtk_widget_set_hexpand(GtkWidget*, gboolean) — (long, int)void
gtk_widget_set_vexpand(GtkWidget*, gboolean) — (long, int)void
```

**Critério:** Arrastar borda da janela → conteúdo HTML redimensiona proporcionalmente.

---

## Estrutura de Arquivos (pós-M1)

```
src/main/java/jauri/
├── Main.java              # Atualizado: janela + webview
├── GtkMainLoop.java       # M0 (inalterado)
└── ffm/
    ├── LibC.java          # M0 (inalterado)
    ├── Gtk3.java          # M0 + gtk_container_add + gtk_widget_set_hexpand/vexpand
    ├── WebKit.java        # NOVO: bindings libwebkit2gtk-4.1.so
    └── JauriWebView.java  # NOVO: wrapper WebView

src/test/java/jauri/
├── ffm/
│   ├── LibCTest.java      # M0 (inalterado)
│   ├── Gtk3Test.java      # M0 (inalterado)
│   └── WebKitTest.java    # NOVO: teste static init
└── M1EndToEndTest.java    # NOVO: HTML inline + URI
```

## Alterações no pom.xml

Nenhuma — WebKitGTK é linked via `SymbolLookup.libraryLookup("libwebkit2gtk-4.1.so", ...)`, mesma abordagem de `libgtk-3.so`. A JVM resolve a lib em tempo de execução via LD_LIBRARY_PATH ou rpath.

## CI (M1 workflow)

Novo `.github/workflows/m1.yml` — copia de m0.yml mas instala `libwebkit2gtk-4.1-dev` (já está no Ubuntu 24.04). E2E test com `-Djauri.e2e=true` como no M0.
