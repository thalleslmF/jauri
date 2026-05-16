# M0.4 — JaureApp: Classe de Ciclo de Vida

**Marco:** M0 — Projeto Java + JNA + GTK Window  
**Dependências:** M0.3  
**Entrega:** App pode ser iniciado e parado programaticamente

## Objetivo

Criar a classe orquestradora `JauriApp` com estado claro.

## Tarefas

1. Criar `jauri/JauriApp.java` com estados:
   - `CREATED` → `start()` → `RUNNING` → `stop()` → `STOPPED`
2. Métodos:
   - `start()`: inicia GtkMainLoop
   - `stop()`: para GtkMainLoop
   - `await()`: bloqueia até STOPPED
3. `main()` só faz `new JauriApp().start().await()`

## Critério de Aceite

Teste que chama `start()` + `stop()` sem abrir janela completa em <1s. Ciclo de vida não lança exceção. Estados seguem ordem correta.
