# ADR-003: Embedding WebView via JNA

**Status:** Aceito  
**Data:** 2026-05-16

## Contexto

Precisamos chamar funções C do GTK3 + WebKitGTK 4.1 do Java. Opções: JNI, JNA, Project Panama.

## Decisão

Usar **JNA 5.14+** para todos os bindings nativos.

Bindings necessários (~100 linhas no total):
- GTK3: `gtk_init`, `gtk_window_new`, `gtk_container_add`, `gtk_widget_show_all`, `gtk_main`, `gtk_main_quit`, `g_signal_connect`
- WebKitGTK: `webkit_web_view_new`, `webkit_web_view_load_html`, `webkit_web_view_load_uri`

## Justificativa

JNA: 1 dependência Maven, zero código C, zero compilação nativa. JNI exige header C + .so separado. Panama precisa JDK 22+ e docs escassas.

## Consequências

**Positivas:** Desenvolvimento rápido, zero compilação nativa, JNA maduro.

**Negativas:** JNA + GraalVM precisa de config extra (jni-config.json). Erro de tipo vira segfault sem stack trace Java.
