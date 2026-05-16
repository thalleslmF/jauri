# M5.2 — JNA + Native Image Config

**Marco:** M5 — GraalVM Native Image  
**Dependências:** M5.1  
**Entrega:** Binário com JNA funcionando sem warnings

## Objetivo

Resolver a configuração especial que JNA precisa pro GraalVM.

## Tarefas

1. Criar `jni-config.json` com as libs nativas:
   ```json
   [{"name":"jauri.jna.Gtk3", "methods":[{"name":"<init>","parameterTypes":[]}]}]
   ```
2. Adicionar `--initialize-at-build-time=jauri.jna` nas flags
3. Garantir que as libs nativas (`libgtk-3.so`, `libwebkit2gtk-4.1.so`) são linkedas
4. Testar: build + run sem warnings JNA

## Critério de Aceite

`native-image` compila sem warnings relacionados a JNA. Binário abre webview e carrega HTML normalmente.