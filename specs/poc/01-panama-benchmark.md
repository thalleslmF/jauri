# PoC: Project Panama vs JNA

**Status:** Planejado  
**Início:** Após M2

## Objetivo

Comparar performance real entre JNA e Project Panama para chamadas de funções GTK3/WebKitGTK. Decidir se vale a pena migrar.

## Setup

- JDK 22+ com `--enable-preview`
- JNA 5.14+ como baseline
- JMH (Java Microbenchmark Harness) para medições precisas

## Bindings a testar

GTK3:
- `gtk_init` — chamada única no startup
- `gtk_window_new` — chamada única no startup
- `gtk_widget_show_all` — chamada única
- `g_signal_connect` — chamada para cada evento

WebKitGTK:
- `webkit_web_view_load_html` — chamada única
- `webkit_web_view_load_uri` — chamada única

WebKit via JavaScript (hot path):
- `webkit_web_view_run_javascript` — chamada a cada interação do usuário (~60fps em animações)

## Métricas

| Métrica | JNA | Panama | Diferença |
|---------|-----|--------|-----------|
| Startup (primeira chamada) | | | |
| Latência média (10k iterações) | | | |
| P95 latência | | | |
| Throughput (chamadas/s) | | | |
| Bytes de código fonte | | | |
| Binário GraalVM | Funciona | N/A | |

## Critério de Decisão

Se Panama mostrar:
- Latência < 30% da JNA
- Código mais limpo
- Throughput > 2x da JNA

...então considerar criar um branch `experimental/panama` e manter ambos durante M3-M4.

Se não:
- Manter JNA como definitivo
- Documentar os números como justificativa

## Procedimento

1. Implementar o mesmo binding GTK mínimo (gtk_init → gtk_window_new → show) nas duas abordagens
2. Rodar JMH com 5 warmup + 10 measurement iterations
3. Publicar resultados no ADR-003

## Arquivos esperados

```
poc/panama/
├── pom.xml (JDK 22+)
├── src/main/java/jauri/poc/PanamaGtk3.java
├── src/main/java/jauri/poc/JnaGtk3.java
└── src/main/java/jauri/poc/BenchmarkRunner.java
```