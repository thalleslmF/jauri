# E2E M0 — Janela GTK

**Marco:** M0  
**Tipo:** Teste end-to-end headless + visual

## Setup

Pré-requisitos:
- `libgtk-3-dev` e `libwebkit2gtk-4.1-dev` instalados
- Xvfb (X virtual framebuffer) para ambientes sem display

## Cenário 1: Janela abre e fecha

1. Iniciar app com `mvn exec:java` (ou `DISPLAY=:99 xvfb-run mvn exec:java`)
2. Verificar que:
   - Processo Java inicia sem crash
   - `gtk_init` retorna sem erro
   - Janela GTK é criada (classe `GtkWindow`)
   - `gtk_main` entra no loop
3. Simular fechamento:
   - Enviar sinal `destroy` via `g_signal_connect`
   - Verificar que `gtk_main_quit` é chamado
   - Verificar que `waitFor()` retorna
   - Verificar exit code 0

## Cenário 2: JNA bindings funcionam

```java
LibC.INSTANCE.getpid() // deve retornar PID real > 0
LibC.INSTANCE.printf("Jauri OK\n") // deve printar no stdout
```

## Cenário 3: Ciclo de vida (sem GTK)

```java
JauriApp app = new JauriApp();
assert app.getState() == State.CREATED;
// app.start() sem GTK (mock) — testar mudança de estado
app.stop();
assert app.getState() == State.STOPPED;
```

## Ferramentas

- JUnit 5 para testes unitários
- Xvfb + `import` (ImageMagick) para capturar screenshot da janela
- `xdotool` para simular clique no X da janela
- `wmctrl -l` para listar janelas abertas

## Critério de Aceite

Testes rodam em CI sem display físico (Xvfb). Nenhum segfault. Exit code 0 sempre.