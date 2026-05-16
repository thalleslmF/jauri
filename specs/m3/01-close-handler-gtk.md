# M3.1 — Close Handler no GTK

**Marco:** M3 — Shutdown limpo + ciclo de vida  
**Dependências:** M0.4  
**Entrega:** Clicar X na janela → shutdown orquestrado

## Objetivo

Detectar quando usuário fecha a janela e disparar shutdown limpo.

## Tarefas

1. Binding de `g_signal_connect(Pointer instance, String signal, GCallback callback, Pointer data)`:
   - Conectar ao sinal `destroy` da janela GTK
2. Criar callback JNA que chama o shutdown do `JauriApp`
3. `JauriApp.stop()` é chamado automaticamente ao fechar janela

## Critério de Aceite

Fechar janela (clicar X) → HTTP server para → GTK main loop termina → processo sai exit 0. Nada de processo zumbi ou porta presa.