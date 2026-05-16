# Jauri

Java WebView engine — concorrente de JCEF e JavaFX WebView. Leve (~15MB), nativo (GraalVM), só Linux.

## Como funciona

```
WebView (WebKitGTK) ← HTTP localhost → Java Backend (GraalVM native-image)
```

A engine abre uma janela nativa com webview embutida, carrega HTML/CSS/JS do seu app, e comunica o frontend com o backend via HTTP localhost.

## Roadmap

| Marco | Entrega | Status |
|-------|---------|--------|
| M0 | Projeto Maven + JNA + GTK abre janela | Spec |
| M1 | WebView carregando HTML | Spec |
| M2 | HTTP Server + IPC Bridge | Spec |
| M3 | Shutdown limpo + ciclo de vida | Spec |
| M4 | App demo SQLite funcional | Spec |
| M5 | GraalVM Native Image (~15MB) | Spec |
| M6 | Polimento + distribuição | Spec |

## Documentação

- `specs/` — especificações de cada marco
- `adrs/` — Architecture Decision Records

## Licença

MIT
