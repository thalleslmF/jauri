
# M4.1 — App Demo: SQLite Query Tool

**Marco:** M4 — App Demo funcional  
**Dependências:** M3.3, JDBC SQLite driver  
**Entrega:** App funcional que executa SQL em SQLite

## TDD (Test-First)

### Teste: Handlers do demo

```java
package demo;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class DemoAppTest {
    private DemoApp demo;
    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        demo = new DemoApp(conn);
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    @Test
    void querySelect1ReturnsOneRow() {
        JsonObject args = new JsonObject();
        args.addProperty("sql", "SELECT 1 AS valor");
        JsonObject result = demo.handleQuery(args);
        
        assertTrue(result.has("columns"));
        assertTrue(result.has("rows"));
        assertEquals(1, result.getAsJsonArray("rows").size());
        assertEquals("1", result.getAsJsonArray("rows")
            .get(0).getAsJsonArray().get(0).getAsString());
    }

    @Test
    void queryInvalidSqlReturnsError() {
        JsonObject args = new JsonObject();
        args.addProperty("sql", "SELECT INVALID");
        assertThrows(Exception.class, () -> demo.handleQuery(args));
    }

    @Test
    void listTablesReturnsArray() throws SQLException {
        // Cria tabela de teste
        conn.createStatement().execute("CREATE TABLE test (id INT)");
        
        JsonObject args = new JsonObject();
        JsonObject result = demo.handleTables(args);
        
        assertTrue(result.has("tables"));
        assertTrue(result.getAsJsonArray("tables").toString().contains("test"));
    }
}
```

## Implementação

### pom.xml (adicionar SQLite)

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.1.0</version>
</dependency>
```

### src/main/java/demo/DemoApp.java

```java
package demo;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import jauri.server.HandlerRegistry;
import java.sql.*;

public class DemoApp {

    private final Connection conn;

    public DemoApp(String dbPath) throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    public void registerHandlers(HandlerRegistry registry) {
        registry.register("db.query", this::handleQuery);
        registry.register("db.tables", this::handleTables);
    }

    public JsonObject handleQuery(JsonObject args) {
        String sql = args.get("sql").getAsString();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            
            JsonArray columns = new JsonArray();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnName(i));
            }
            
            JsonArray rows = new JsonArray();
            while (rs.next()) {
                JsonArray row = new JsonArray();
                for (int i = 1; i <= colCount; i++) {
                    row.add(rs.getString(i));
                }
                rows.add(row);
            }
            
            JsonObject result = new JsonObject();
            result.add("columns", columns);
            result.add("rows", rows);
            result.addProperty("rowCount", rows.size());
            
            // Fecha explicitamente o Statement/ResultSet
            // (try-with-resources já faz isso)
            
            return result;
            
        } catch (SQLException e) {
            throw new RuntimeException("Query error: " + e.getMessage());
        }
    }

    public JsonObject handleTables(JsonObject args) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            JsonArray tables = new JsonArray();
            
            try (ResultSet rs = meta.getTables(null, null, "%",
                    new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            
            JsonObject result = new JsonObject();
            result.add("tables", tables);
            return result;
            
        } catch (SQLException e) {
            throw new RuntimeException("Tables error: " + e.getMessage());
        }
    }

    public void close() {
        try { conn.close(); } catch (SQLException ignored) {}
    }
}
```

### static/index.html (demo SQLite)

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Jauri SQLite Demo</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            background: #1a1a2e; color: #e0e0e0;
            font-family: 'Segoe UI', system-ui, sans-serif;
            padding: 20px;
        }
        .container { max-width: 900px; margin: 0 auto; }
        h1 { color: #00d4aa; margin-bottom: 20px; }
        .sql-input {
            width: 100%;
            padding: 12px;
            background: #16213e;
            border: 1px solid #0f3460;
            border-radius: 8px;
            color: #00d4aa;
            font-family: 'Courier New', monospace;
            font-size: 14px;
            margin-bottom: 10px;
            resize: vertical;
        }
        .sql-input:focus { outline: none; border-color: #00d4aa; }
        .btn-row { display: flex; gap: 10px; margin-bottom: 20px; }
        button {
            padding: 10px 20px;
            border: none;
            border-radius: 8px;
            font-size: 14px;
            font-weight: bold;
            cursor: pointer;
            transition: transform 0.2s;
        }
        .btn-execute { background: #00d4aa; color: #1a1a2e; }
        .btn-tables { background: #0f3460; color: #e0e0e0; }
        button:hover { transform: scale(1.03); }
        table {
            width: 100%;
            border-collapse: collapse;
            background: #16213e;
            border-radius: 8px;
            overflow: hidden;
        }
        th {
            background: #0f3460;
            color: #00d4aa;
            padding: 12px;
            text-align: left;
            font-weight: 600;
        }
        td {
            padding: 10px 12px;
            border-top: 1px solid #0f3460;
            font-family: 'Courier New', monospace;
            font-size: 13px;
        }
        tr:hover td { background: #1a2744; }
        .error { color: #e74c3c; padding: 12px; background: #2d1b1b; border-radius: 8px; }
        .info { color: #a0a0b0; margin-bottom: 10px; font-size: 13px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>Jauri SQLite Demo</h1>
        <div class="info">Conectado ao SQLite em memoria. Execute qualquer SQL.</div>
        <textarea id="sqlInput" class="sql-input" rows="3"
            placeholder="Digite sua SQL aqui...">SELECT 'Hello, Jauri!' AS greeting</textarea>
        <div class="btn-row">
            <button class="btn-execute" onclick="executeSql()">Executar SQL</button>
            <button class="btn-tables" onclick="listTables()">Listar Tabelas</button>
        </div>
        <div id="result"></div>
    </div>
    <script>
        async function invoke(cmd, args = {}) {
            const resp = await fetch('/api/invoke', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ cmd, args })
            });
            return resp.json();
        }

        function renderResult(data) {
            const div = document.getElementById('result');
            if (data.error) {
                div.innerHTML = '<div class="error">' + data.error + '</div>';
                return;
            }
            if (data.result && data.result.tables) {
                div.innerHTML = '<pre>Tables: ' + data.result.tables.join(', ') + '</pre>';
                return;
            }
            if (data.result && data.result.columns) {
                let html = '<table><thead><tr>';
                data.result.columns.forEach(c => html += '<th>' + c + '</th>');
                html += '</tr></thead><tbody>';
                data.result.rows.forEach(r => {
                    html += '<tr>';
                    r.forEach(v => html += '<td>' + (v || 'NULL') + '</td>');
                    html += '</tr>';
                });
                html += '</tbody></table>';
                html += '<div class="info">' + data.result.rowCount + ' row(s)</div>';
                div.innerHTML = html;
            }
        }

        async function executeSql() {
            const sql = document.getElementById('sqlInput').value;
            document.getElementById('result').textContent = 'Executando...';
            const data = await invoke('db.query', { sql });
            renderResult(data);
        }

        async function listTables() {
            document.getElementById('result').textContent = 'Buscando...';
            const data = await invoke('db.tables', {});
            renderResult(data);
        }

        // Auto-executa on load
        executeSql();
    </script>
</body>
</html>
```

## Critério de Aceite

```bash
mvn test -Dtest=DemoAppTest
mvn exec:java  # DemoApp como mainClass
# Janela abre → SQL "SELECT 1 AS valor" → tabela aparece com "1" na coluna "valor"
```
