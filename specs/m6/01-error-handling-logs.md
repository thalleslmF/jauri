# M6.1 — Tratamento de Erros + Logs

**Marco:** M6 — Polimento + distribuição  
**Dependências:** M5  
**Entrega:** Erros claros e logs úteis

## Objetivo

Mensagens de erro descritivas e logging básico.

## Tarefas

1. Adicionar `java.util.logging` (ou log4j se preferir)
2. Logs:
   - INFO: startup, porta do HTTP server, shutdown
   - WARNING: handler não encontrado, requisição lenta
   - SEVERE: erro JNA, falha ao iniciar GTK
3. Se `libwebkit2gtk-4.1` não estiver instalada:
   - Printar mensagem clara: "Jauri requires libwebkit2gtk-4.1. Install: sudo apt install libwebkit2gtk-4.1-dev"
4. Erros de JNA viram exceções Java descritivas (try/catch no startup)

## Critério de Aceite

Rodar sem webkit2gtk → mensagem clara de erro. Rodar normal → logs limpos. Stdout sem poluição.