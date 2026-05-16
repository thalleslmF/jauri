# M0.2 — JNA Binding: GTK 3 Mínimo

**Marco:** M0 — Projeto Java + JNA + GTK Window  
**Dependências:** M0.1, `libgtk-3-dev` instalado  
**Entrega:** Janela vazia aparece na tela

## Objetivo

Criar bindings JNA para as funções mínimas do GTK3 e abrir uma janela.

## Tarefas

1. Criar `jauri/jna/Gtk3.java` com interface JNA contendo:
   - `gtk_init(int argc, String[] argv)`
   - `gtk_window_new(int type)` → `Pointer` (GtkWidget)
   - `gtk_widget_show_all(Pointer widget)`
   - `gtk_main()`
   - `gtk_main_quit()`
2. Em `Main.java`, chamar a sequência: init → window → show → main
3. Fechar com Ctrl+C

## Critério de Aceite

Rodar `mvn exec:java` → janela vazia aparece na tela. Fechar com Ctrl+C encerra o processo.

## Constantes

```java
int GTK_WINDOW_TOPLEVEL = 0;
```

## Notas

GTK precisa ser inicializado antes de qualquer outra chamada. A thread que chama `gtk_main` é a thread principal.
