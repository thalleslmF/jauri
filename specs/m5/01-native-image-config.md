# M5.1 — Native Image com Reflection Config

**Marco:** M5 — GraalVM Native Image  
**Dependências:** M4, GraalVM + native-image instalado  
**Entrega:** Binário compilado que abre a webview

## Objetivo

Compilar o Jauri runtime com GraalVM native-image.

## Tarefas

1. Criar `META-INF/native-image/jauri/reflect-config.json`:
   - Classes do Gson (JsonObject, Gson)
   - Classes do JNA (Library, Native, Pointer)
   - Classes do HTTP server (HttpServer, HttpExchange)
   - Handlers registrados no registry
2. Configurar `pom.xml` com `native-image-maven-plugin` (opcional) ou script manual
3. Criar `scripts/build-native.sh`:
   ```bash
   native-image -jar target/jauri.jar jauri      --initialize-at-build-time      -H:ReflectionConfigurationFiles=reflect-config.json
   ```

## Critério de Aceite

`./scripts/build-native.sh` → `./jauri` abre o app demo. Binário executa sem JDK.