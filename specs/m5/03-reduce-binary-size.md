# M5.3 — Reduzir Tamanho do Binario

**Marco:** M5 — GraalVM Native Image  
**Dependencias:** M5.2  
**Entrega:** Binario < 20MB

## Estrategia

1. **Analisar**: `native-image --verbose` + `-H:+PrintImageObjectTree`
2. **Remover dependencias**: garantir que pom.xml so tem o essencial
3. **Flags de otimizacao**: `-O2` (optimize), `--gc=G1` (garbage collector leve)
4. **Remover debug symbols**: `-H:-DeleteLocalSymbols` (manter so o necessario)

## Implementacao

### scripts/build-native.sh (otimizado)

```bash
native-image \
    -jar target/jauri-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
    jauri \
    -O2 \
    --gc=G1 \
    --initialize-at-build-time \
    --initialize-at-run-time=jauri.jna.Gtk3,jauri.jna.WebKit \
    -H:ReflectionConfigurationResources=META-INF/native-image/jauri/reflect-config.json \
    -H:JNIConfigurationResources=META-INF/native-image/jauri/jni-config.json \
    -H:+ReportExceptionStackTraces \
    -H:-DeleteLocalSymbols \
    --no-fallback \
    --enable-url-protocols=http \
    2>&1 | tee target/native-build.log
```

## Metrica

```bash
ls -lh jauri
# Target: < 20MB
```

### Analise de tamanho

```bash
# Usar para debugar o que mais pesa
native-image --verbose -H:+PrintImageObjectTree ...
```

## Criterio de Aceite

`ls -lh jauri` mostra < 20MB. App demo SQLite roda normalmente.
