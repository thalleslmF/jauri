# M0.3 — GtkMainLoop em Thread Separada

**Marco:** M0 — Projeto Java + JNA + GTK Window  
**Dependências:** M0.2  
**Entrega:** Fechar janela → programa termina com exit code 0

## Objetivo

Encapsular o loop GTK numa classe que gerencia startup e shutdown.

## Tarefas

1. Criar `jauri/gui/GtkMainLoop.java`:
   - `start()`: chama `gtk_init`, cria janela, chama `gtk_main`
   - `stop()`: chama `gtk_main_quit` via `g_idle_add`
   - `waitFor()`: bloqueia até o loop terminar
2. Usar `g_idle_add` pra chamar `gtk_main_quit` de forma segura (da thread GTK)
3. Binding de `g_idle_add` e `g_signal_connect` (destroy event)

## Critério de Aceite

Fechar janela → `gtk_main_quit` é chamado → `waitFor()` retorna → programa termina exit 0. Não fica processo zumbi.

## Notas

`g_idle_add` agenda uma função pra rodar na próxima iteração do loop GTK. É a forma segura de modificar estado GTK de fora da thread principal.
