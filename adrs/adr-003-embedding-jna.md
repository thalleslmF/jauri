# ADR-003: Embedding WebView via JNA

**Status:** Aceito (com experimentos paralelos)  
**Data:** 2026-05-16

## Contexto

Precisamos chamar funções C do GTK3 + WebKitGTK 4.1 a partir do Java para criar uma janela nativa com webview embutida. As opções disponíveis são JNA, JNI, e Project Panama.

## Decisão

**Estratégia primária:** JNA 5.14+ para todos os bindings nativos.

**Pesquisa paralela:** PoCs com Project Panama (JDK 22+) para comparar performance e latência de chamadas.

## Bindings necessários (~100 linhas)

GTK3:
- `gtk_init(int, String[])` — inicializa o toolkit GTK
- `gtk_window_new(int)` — cria uma janela
- `gtk_container_add(Pointer, Pointer)` — adiciona widget ao container
- `gtk_widget_show_all(Pointer)` — exibe todos os widgets
- `gtk_main()` — entra no loop de eventos GTK
- `gtk_main_quit()` — sai do loop de eventos
- `g_signal_connect(Pointer, String, Callback, Pointer)` — conecta eventos (destroy, configure)

WebKitGTK:
- `webkit_web_view_new()` — cria uma WebView
- `webkit_web_view_load_html(Pointer, String, String)` — carrega HTML inline
- `webkit_web_view_load_uri(Pointer, String)` — carrega URL

## Comparação das abordagens

```
                JNA          JNI             Panama
Dependência    1 (jna.jar)  Nenhuma         Nenhuma (JDK)
Código C       Zero         Header .h + .so Zero
Compilação     Não          Sim (gcc)       Sim (java --enable-preview)
Debug          Stack Java   Segfault opaco  Stack Java + JVM crash
GraalVM        Config extra Config extra    NÃO SUPORTADO
JDK mínimo     8+           8+              22+
Maturidade     15+ anos     20+ anos        Experimental (incubation)
Latência       ~2µs/call    ~0.3µs/call     ~0.5µs/call
```

**JNA:**
- Vantagem: 1 linha no pom.xml, zero código nativo, debug via stack trace Java
- Desvantagem: mais lento que JNI/Panama (~6x vs JNI), config extra no GraalVM
- Ideal pra: prototipação e bindings de pocas funções (~30 calls/s)

**JNI:**
- Vantagem: performance máxima, padrão Java desde sempre
- Desvantagem: exige header C + compilação separada + .so específico por arquitetura. Um binding errado = segfault sem informação. Cada função nova = header + implementação C + recompilação
- Ideal pra: hot paths com 1000+ chamadas/segundo

**Project Panama (JDK 22+):**
- Vantagem: performance próxima de JNI, sintaxe limpa (MethodHandles), sem código C
- Desvantagem: JDK 22+ obrigatório, GraalVM não suporta Panama em native-image, API ainda mudando entre versões
- Ideal pra: projetos JDK-only sem GraalVM

## PoC Planejada

| Aspecto | JNA | Panama |
|---------|-----|--------|
| Startup (primeira chamada) | Medir | Medir |
| Latência por chamada (média 10k iters) | Medir | Medir |
| Latência em hot path (webview redraw ~60fps) | Medir | Medir |
| Bytes de reflection metadata no native-image | Medir | N/A |
| Código fonte (linhas) | Contar | Contar |

**Critério de aceite dos PoCs:** Se Panama mostrar latência < 30% da JNA com código mais limpo, considerar migrar para um branch experimental. Decisão final após M4.

## Consequências

**Positivas:** Desenvolvimento rápido com JNA, experimento Panama documentado, decisão baseada em dados (não achismo).

**Negativas:** Duas implementações de binding pra manter durante a fase de PoC. GraalVM é o target de produção, então Panama só seria viável se abandonarmos native-image.
