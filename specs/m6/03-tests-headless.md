# M6.3 — Testes Automatizados (Headless)

**Marco:** M6 — Polimento + distribuição  
**Dependências:** M5  
**Entrega:** `mvn test` roda sem GTK em < 10s

## Objetivo

Testes que não dependem de display gráfico.

## Tarefas

1. Testes do `HandlerRegistry`:
   - Registrar handler, chamar, ver resultado
   - Handler não encontrado → exceção
2. Testes do `JauriHttpServer`:
   - Iniciar, fazer requisição, ver resposta
   - Parar, verificar que parou
3. Testes do ciclo de vida (`JauriApp`):
   - Mockar GtkMainLoop
   - start() → estado RUNNING
   - stop() → estado STOPPED
4. Testes do `StaticFileHandler`:
   - Servir arquivo existente → 200 + conteúdo
   - Arquivo não existe → 404

## Critério de Aceite

`mvn test` roda 20+ testes em < 10s. Nenhum teste precisa de display gráfico (XDG/Virtual framebuffer).