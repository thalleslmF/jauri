# M6.3 — Testes Automatizados (Headless)

**Marco:** M6 — Polimento + distribuicao  
**Dependencias:** M5  
**Entrega:** `mvn test` roda sem GTK em < 10s

## Objetivo

Todos os testes devem rodar sem display grafico (sem Xvfb, sem X11).
Classes que precisam de GTK/WebKit sao mockadas ou testadas com condicional `@EnabledIfEnvironmentVariable`.

## Testes Headless (21+ testes)

### 1. HandlerRegistryTest (5 testes)

```java
- registrar handler, chamar, ver resultado
- comando nao registrado -> excecao
- multiplos handlers registrados
- sobrescrita de handler (overwrite)
- hasCommand() true/false
```

**Arquivo**: `src/test/java/jauri/server/HandlerRegistryTest.java`

### 2. JauriHttpServerTest (5 testes)

```java
- start() -> health check retorna 200
- stop() -> isRunning false
- porta muda a cada execucao
- POST /api/invoke com ping -> 200 + "pong"
- POST /api/invoke com comando invalido -> 404
```

**Arquivo**: `src/test/java/jauri/server/JauriHttpServerTest.java`

### 3. StaticFileHandlerTest (4 testes)

```java
- servir arquivo existente -> 200 + Content-Type correto
- arquivo nao existe -> 404
- path traversal (../etc/passwd) -> 403
- root (/) -> index.html
```

**Arquivo**: `src/test/java/jauri/server/StaticFileHandlerTest.java`

### 4. ApiInvokeHandlerTest (4 testes)

```java
- ping command -> {"result":{"message":"pong"}}
- comando desconhecido -> error no JSON
- metodo GET -> 405
- JSON invalido no body -> 400
```

**Arquivo**: `src/test/java/jauri/server/ApiInvokeHandlerTest.java`

### 5. Ciclo de Vida (3 testes)

```java
- JauriApp.testStart() -> state RUNNING
- stop() -> state STOPPED
- stop sem start -> noop (nao lanca excecao)
```

**Arquivo**: `src/test/java/jauri/JauriAppTest.java`

## Total esperado: ~21 testes em < 10s

```bash
mvn test -q
# Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
# [INFO] BUILD SUCCESS
```

### Testes que REQUEREM display (executados separadamente)

```bash
# Opcional: testes de integracao GTK (requerem display/xvfb)
xvfb-run mvn test -Dtest="*IntegrationTest" -pl .
# Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

## Criterio de Aceite

`mvn test` roda 20+ testes em < 10s. Nenhum teste precisa de display grafico (XDG/Virtual framebuffer). Testes de integracao GTK sao separados e opcionais.
