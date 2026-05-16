# M5.1 — Native Image com Reflection Config

**Marco:** M5 — GraalVM Native Image  
**Dependencias:** M4, GraalVM + native-image instalado  
**Entrega:** Binario compilado que abre a webview

## TDD (Test-First)

### Teste: Build script existe e e executavel

```java
@Test
void buildScriptExists() {
    File script = new File("scripts/build-native.sh");
    assertTrue(script.exists());
    assertTrue(script.canExecute());
}
```

## Implementacao

### reflect-config.json

`src/main/resources/META-INF/native-image/jauri/reflect-config.json`:

```json
[
  {"name":"com.sun.net.httpserver.HttpServer","methods":[{"name":"<init>","parameterTypes":[]}]},
  {"name":"com.sun.net.httpserver.HttpExchange"},
  {"name":"com.google.gson.Gson"},
  {"name":"com.google.gson.JsonObject"},
  {"name":"com.google.gson.JsonArray"},
  {"name":"com.google.gson.JsonPrimitive"},
  {"name":"org.sqlite.JDBC"},
  {"name":"org.sqlite.core.DB"},
  {"name":"jauri.jna.Gtk3"},
  {"name":"jauri.jna.WebKit"},
  {"name":"jauri.jna.LibC"}
]
```

### scripts/build-native.sh

```bash
#!/bin/bash
set -euo pipefail

echo "[Jauri] Building native image..."
echo "[Jauri] This requires GraalVM with native-image installed."

# Build JAR
mvn package -DskipTests -q

# Build native image
native-image \
    -jar target/jauri-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
    jauri \
    --initialize-at-build-time \
    --initialize-at-run-time=jauri.jna.Gtk3,jauri.jna.WebKit \
    -H:ReflectionConfigurationResources=reflect-config.json \
    -H:+ReportExceptionStackTraces \
    --no-fallback \
    --verbose \
    2>&1 | tee target/native-build.log

echo "[Jauri] OK Native image built: ./jauri"
```

## Criterio de Aceite

`./scripts/build-native.sh` -- `./jauri` abre o app demo. Binario executa sem JDK.

## Dependencias de Sistema

```bash
# Instalar GraalVM + native-image
# Download de https://github.com/graalvm/graalvm-ce-builds/releases
export JAVA_HOME=/path/to/graalvm
export PATH=$JAVA_HOME/bin:$PATH
gu install native-image
```
