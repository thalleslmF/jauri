# M6.2 — README + Exemplos

**Marco:** M6 — Polimento + distribuicao  
**Dependencias:** M6.1  
**Entrega:** README completo + exemplos + INSTALL

## Implementacao

### README.md (conteudo final)

```markdown
# Jauri

> Java + WebKitGTK = Desktop apps com HTML/CSS/JS e backend Java nativo

Jauri e um runtime Java para construir aplicacoes desktop usando webviews.
O backend e Java puro (via JNA/WebKitGTK), a UI e HTML/CSS/JS moderno,
e a comunicacao e via HTTP local (IPC).

## Quickstart (5 passos)

```bash
# 1. Compilar e rodar em modo dev
mvn compile exec:java

# 2. Abre janela com HTML renderizado
# 3. Botao "Ping Backend" -- backend Java responde "pong"
```

## Estrutura

```
jauri/
  src/main/java/jauri/        # Runtime core
    JauriApp.java             # Ciclo de vida (start/stop/await)
    jna/                      # Bindings JNA
      LibC.java               # libc
      Gtk3.java               # GTK3
      WebKit.java             # WebKitGTK
    gui/                      # GUI
      GtkMainLoop.java        # Thread GTK + loop
      Window.java             # Janela + callbacks
    server/                   # HTTP Server (IPC)
      JauriHttpServer.java
      ApiInvokeHandler.java
      StaticFileHandler.java
      HandlerRegistry.java
    webview/                  # WebView Engine
      WebViewEngine.java
      StaticAssets.java
  src/main/resources/static/  # Frontend HTML/CSS/JS
  src/test/java/              # Testes
```

## Exemplo: Handler Customizado

No backend Java:
```java
registry.register("hello", args -> {
    JsonObject result = new JsonObject();
    String name = args.get("name").getAsString();
    result.addProperty("message", "Ola, " + name + "!");
    return result;
});
```

No frontend JS:
```js
const data = await fetch('/api/invoke', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ cmd: 'hello', args: { name: 'Jauri' } })
});
```

## Requisitos

- Java 11+
- Maven 3.8+
- libgtk-3-dev
- libwebkit2gtk-4.1-dev

## Licenca

MIT
```

### INSTALL.md

```markdown
# Instalacao

## Linux (Ubuntu/Debian)

### 1. Dependencias de sistema

```bash
sudo apt update
sudo apt install -y libgtk-3-dev libwebkit2gtk-4.1-dev
```

### 2. Java + Maven

```bash
sudo apt install -y openjdk-11-jdk maven
```

### 3. Compilar e rodar

```bash
git clone https://github.com/thalleslmF/jauri
cd jauri
mvn compile
mvn exec:java
```

## Distribuicao (binario nativo)

```bash
# Download
wget https://github.com/thalleslmF/jauri/releases/latest/download/jauri-linux-x64.zip
unzip jauri-linux-x64.zip
cd jauri-*

# Rodar (dependencias de sistema ja instaladas)
./jauri
```
```

## Criterio de Aceite

Dev novo le README -- cria app Jauri funcional em < 10 minutos. README cobre: o que e, quickstart, estrutura de diretorios, exemplo de handler, exemplo de JS.
