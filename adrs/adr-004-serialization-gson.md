# ADR-004: Serialização com Gson

**Status:** Aceito  
**Data:** 2026-05-16

## Contexto

IPC usa JSON. Precisamos de uma lib de serialização.

## Decisão

Usar **Gson** (`com.google.code.gson:gson:2.10+`).

## Justificativa

Menos reflection implícita que Jackson (mais fácil de configurar pro GraalVM). API mínima: `new Gson()` + `toJson()` / `fromJson()`. Suporte a `JsonObject` pra args dinâmicos.

## Consequências

**Positivas:** Reflection config pro GraalVM explícita e pequena.

**Negativas:** Performance ~2x menor que Jackson. Irrelevante pra apps desktop.
