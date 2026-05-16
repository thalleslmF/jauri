# M2.4 — HandlerRegistry: Registrar Comandos

**Marco:** M2 — HTTP Server + IPC Bridge  
**Dependências:** M2.3  
**Entrega:** Apps registram handlers com nome, engine roteia

## Objetivo

Criar um registry que mapeia nomes de comando pra funções Java.

## Tarefas

1. Criar `jauri/server/HandlerRegistry.java`:
   - `register(String cmd, JaureHandler handler)`: registra handler
   - `handle(String cmd, JsonObject args)`: executa handler, retorna JsonObject
   - Lança exceção se comando não encontrado
2. Criar interface funcional `jauri/server/JauriHandler.java`:
   - `JsonObject handle(JsonObject args)`
3. Conectar com `ApiInvokeHandler`: recebe JSON → roteia → retorna resultado

## Exemplo de uso

```java
registry.register("hello", args -> {
    JsonObject result = new JsonObject();
    result.addProperty("message", "Olá, " + args.get("name").getAsString());
    return result;
});
```