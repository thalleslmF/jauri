# M5.2 — JNA + Native Image Config

**Marco:** M5 — GraalVM Native Image  
**Dependencias:** M5.1  
**Entrega:** Binario com JNA funcionando sem warnings

## Implementacao

### jni-config.json

`src/main/resources/META-INF/native-image/jauri/jni-config.json`:

```json
[
  {"name":"java.lang.ClassLoader"},
  {"name":"java.lang.reflect.Method"},
  {"name":"com.sun.jna.Native", "methods":[{"name":"<init>","parameterTypes":[]}]},
  {"name":"com.sun.jna.Pointer"},
  {"name":"com.sun.jna.CallbackReference"},
  {"name":"com.sun.jna.Function"},
  {"name":"com.sun.jna.NativeLibrary"}
]
```

### resource-config.json

```json
{
  "resources":[
    {"pattern":"\\\\Qstatic/\\\\E"},
    {"pattern":"\\\\Qindex.html\\\\E"}
  ],
  "bundles":[]
}
```

### proxy-config.json

```json
[
  ["com.sun.jna.Library"],
  ["jauri.jna.Gtk3"],
  ["jauri.jna.WebKit"],
  ["jauri.jna.LibC"]
]
```

### scripts/build-native.sh (atualizado)

```bash
#!/bin/bash
set -euo pipefail

echo "[Jauri] Building native image..."

mvn package -DskipTests -q

# Copia configs pro classpath do native-image
mkdir -p target/classes/META-INF/native-image/jauri/
cp src/main/resources/META-INF/native-image/jauri/*.json \
   target/classes/META-INF/native-image/jauri/

native-image \
    -jar target/jauri-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
    jauri \
    --initialize-at-build-time \
    --initialize-at-run-time=jauri.jna.Gtk3,jauri.jna.WebKit \
    -H:ReflectionConfigurationResources=META-INF/native-image/jauri/reflect-config.json \
    -H:JNIConfigurationResources=META-INF/native-image/jauri/jni-config.json \
    -H:ResourceConfigurationResources=META-INF/native-image/jauri/resource-config.json \
    -H:+ReportExceptionStackTraces \
    --no-fallback \
    --verbose \
    2>&1 | tee target/native-build.log

echo "[Jauri] OK Native image: ./jauri"
```

## Criterio de Aceite

`native-image` compila sem warnings relacionados a JNA. Binario abre webview e carrega HTML. Sem ClassNotFoundException em tempo de execucao.
