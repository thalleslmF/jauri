# M4.1 — App Demo: SQLite Query Tool

**Marco:** M4 — App Demo funcional  
**Dependências:** M3.3, JDBC SQLite driver  
**Entrega:** App funcional que executa SQL em SQLite

## Objetivo

Validar que Jauri serve pra construir apps reais. Um mini DB client com SQLite.

## Tarefas

1. Adicionar `org.xerial:sqlite-jdbc:3.45.1.0` no pom.xml
2. Criar `demo/DemoApp.java` com:
   - Handler `db.query`: recebe `{"sql": "SELECT 1"}`, executa via JDBC, retorna resultados
   - Handler `db.tables`: lista tabelas do banco SQLite
3. Criar HTML em `static/index.html`:
   - Campo de texto pra SQL
   - Botão "Executar"
   - Tabela de resultados com colunas e linhas
   - CSS escuro (tema banco de dados)

## Critério de Aceite

`mvn exec:java` com a demo → janela abre → digitar `SELECT 1 AS valor` → clicar Executar → tabela aparece com "1" na coluna "valor".