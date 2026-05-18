# M0 — Janela GTK com Panama FFM — Implementation Plan

> **Stack:** Java 24 GraalVM CE + Panama FFM (JEP 454, final no JDK 24) + GTK 3.24 + JUnit 5 + xvfb-run
> **Ref:** ADR-003 (Panama FFM venceu benchmark JMH), `panic-ffm-benchmark-adr` skill

**Goal:** Binário Java que abre uma janela GTK vazia (800×600), fecha limpo com exit code 0.

**Architecture:** Bindings nativos via Panama FFM (`java.lang.foreign.Linker` + `SymbolLookup`) direto em `libgtk-3.so` e `libgobject-2.0.so`. Sem JNA, sem JNI, sem código C auxiliar. GTK roda na thread principal; shutdown via callback `destroy` conectado com `g_signal_connect_data`.

**Package:** `jauri.ffm` (FFM = Foreign Function & Memory — não `native`, que é palavra reservada em Java).

---

### Task 0: Branch + infra

```bash
cd ~/jauri
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 24.0.2-graalce
git checkout main && git pull origin main
git checkout -b feat/m0-panama-gtk
git push origin feat/m0-panama-gtk
```

### Task 1: M0.1 — Scaffold Maven + Main.java

**Files:**
- `pom.xml` — JDK 24, compiler-plugin 3.13.0, surefire 3.2.5, JUnit 5.11.4
- `src/main/java/jauri/Main.java` — "Jauri M0" println

**Key decisions:**
- `--enable-preview` não é necessário (Panama FFM é final no JDK 24)
- Compiler `<source>24</source>` + `<target>24</target>`
- Surefire precisa de `--add-modules java.base` (já é default, mas explícito)

### Task 2: M0.2 — LibC binding (Panama warm-up) + test

**Files:**
- `src/main/java/jauri/ffm/LibC.java` — binding FFM para `getpid()` de `libc.so.6`
- `src/test/java/jauri/ffm/LibCTest.java` — verifica que PID > 0

**Pattern:** `SymbolLookup.libraryLookup("libc.so.6", arena)` + `linker.downcallHandle` + `FunctionDescriptor.of(ValueLayout.JAVA_INT)`. Arena `ofShared()` no static init.

### Task 3: M0.3 — GTK 3 bindings + test

**Files:**
- `src/main/java/jauri/ffm/Gtk3.java` — bindings Panama FFM para `libgtk-3.so` + `libgobject-2.0.so`
- `src/test/java/jauri/ffm/Gtk3Test.java` — verifica static init + constantes

**Bindings implementados:**
- `gtk_init(int*, char***)` — `(long, long)void`
- `gtk_window_new(GtkWindowType)` — `(int)long`
- `gtk_window_set_default_size(GtkWindow*, int, int)` — `(long, int, int)void`
- `gtk_widget_show_all(GtkWidget*)` — `(long)void`
- `gtk_main()` — `()void`
- `gtk_main_quit()` — `()void`
- `g_signal_connect_data(gpointer, gchar*, GCallback, gpointer, GClosureNotify, GConnectFlags)` — `(long, long, long, long, long, int)long`

**Upcall stub:** `MethodHandles.lookup().findStatic(Gtk3.class, "gtkMainQuit", ...)` → `linker.upcallStub(...)` armazenado em arena estática `STUB_ARENA` de `ofShared()` que vive pro JVM inteiro.

**Pointer layout:** `ValueLayout.JAVA_LONG` para ponteiros (x86-64 ABI). NÃO usar `ValueLayout.ADDRESS` que retorna `MemorySegment`.

### Task 4: M0.4 — GtkMainLoop lifecycle + test

**Files:**
- `src/main/java/jauri/GtkMainLoop.java` — estados `CREATED → RUNNING → STOPPED`
- `src/test/java/jauri/GtkMainLoopTest.java` — 5 testes de lifecycle (sem GTK real)

**Estados:**
- `CREATED` — pós-construção
- `RUNNING` — após `start()` bem-sucedido (e antes de `gtk_main` bloquear)
- `STOPPED` — após `gtk_main_quit` → `gtk_main` retornar

**Guards:**
- `stop()` antes de `start()` → vai direto pra STOPPED
- `start()` depois de STOPPED → `IllegalStateException`
- `start()` se já RUNNING → `IllegalStateException`
- `stop()` duas vezes → safe (idempotente)

### Task 5: M0 E2E — xvfb-run test completo

**Files:**
- `src/test/java/jauri/M0EndToEndTest.java` — 3 testes (1 condicional)

**Testes:**
1. `libCWorks()` — `LibC.getpid()` > 0
2. `gtkBindingsLoad()` — class loading do `Gtk3` não lança
3. `windowOpensAndCloses()` — `@EnabledIfSystemProperty(named = "jauri.e2e")`
   - Abre janela GTK via `GtkMainLoop.start()`
   - Scheduler thread separate chama `loop.stop()` após 500ms
   - `gtk_main_quit()` é thread-safe (GLib docs)
   - Verifica estado `STOPPED` após `start()` retornar

### Task 6: CI — GitHub Actions

**File:** `.github/workflows/m0.yml`

```yaml
name: M0 — GTK + Panama FFM

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: sdkman/sdkman-action@v1
        with:
          candidate: java
          version: 24.0.2-graalce
      - name: Install GTK3 + xvfb
        run: sudo apt-get update && sudo apt-get install -y libgtk-3-dev xvfb
      - name: Build + test (unit)
        run: xvfb-run mvn clean test
      - name: E2E test
        run: xvfb-run mvn test -Djauri.e2e=true -Dtest=M0EndToEndTest#windowOpensAndCloses
```

---

## File Tree (pós-implementação)

```
jauri/
├── pom.xml                                          (M0.1)
├── .github/workflows/m0.yml                         (M0 CI)
├── src/
│   ├── main/java/jauri/
│   │   ├── Main.java                                (M0.1)
│   │   ├── GtkMainLoop.java                         (M0.4)
│   │   └── f fm/
│   │       ├── LibC.java                            (M0.2)
│   │       └── Gtk3.java                            (M0.3)
│   └── test/java/jauri/
│       ├── f fm/
│       │   ├── LibCTest.java                        (M0.2)
│       │   └── Gtk3Test.java                        (M0.3)
│       ├── GtkMainLoopTest.java                     (M0.4)
│       └── M0EndToEndTest.java                      (M0 E2E)
```

## Pitfalls

1. **Arena lifetime for upcall stubs:** A arena que segura um upcall stub precisa sobreviver à conexão do sinal. Use `Arena.ofShared()` estática em vez de `Arena.ofConfined()` dentro do método.

2. **`g_signal_connect_data` retorna `gulong`:** O FunctionDescriptor é `(long,long,long,long,long,int)long`, não `void`. Sempre caste o resultado: `long handlerId = (long) ...invokeExact(...)`.

3. **Pointer layout:** Sempre usar `ValueLayout.JAVA_LONG` para ponteiros GTK (x86-64). `ValueLayout.ADDRESS` retorna `MemorySegment` — incompatível com chamadas que esperam `long`.

4. **xvfb-run:** Testes que criam janela GTK precisam de `xvfb-run`. O E2E real (`windowOpensAndCloses`) é condicional via `@EnabledIfSystemProperty(named = "jauri.e2e")`.

5. **`native` é palavra reservada:** Nome de package `jauri.native` não compila no JDK 24. Usar `jauri.ffm`.

6. **GraalVM 24 + `allocateFrom`:** `arena.allocateFrom("string")` funciona no GraalVM CE 24 (retorna `MemorySegment`). `allocateUtf8String` NÃO existe no GraalVM.

7. **GTK threading:** `gtk_main()` bloqueia a thread. `gtk_main_quit()` é thread-safe (GLib docs) — pode ser chamado de qualquer thread.
