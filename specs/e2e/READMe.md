# Testes End-to-End

Cada marco tem seu próprio arquivo de E2E testando a entrega como um todo, não componentes isolados.

## Estrutura

```
specs/e2e/
├── README.md
├── 01-m0-gtk-window.md
├── 02-m1-webview-html.md
├── 03-m2-ipc-http.md
├── 04-m3-shutdown.md
├── 05-m4-demo-sqlite.md
├── 06-m5-native-image.md
└── 07-m6-polish.md
```

## Ferramentas

- **JUnit 5** — testes unitários e de integração
- **Xvfb** — display virtual pra CI (sem monitor físico)
- **xdotool** — simular clique e redimensionamento de janela
- **webkit_web_view_run_javascript** — ler estado do DOM nos testes
- **lsof / netstat** — detectar leaks de socket
- **strace** (opcional) — detectar leaks de file descriptor
- **ImageMagick import** — capturar screenshot pra testes visuais

## Como rodar

```bash
# Testes unitários (sem GTK)
mvn test

# Testes com display virtual (CI)
xvfb-run mvn verify

# Testes manuais (com display real)
mvn exec:java -Pdemo
```