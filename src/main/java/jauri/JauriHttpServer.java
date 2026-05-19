package jauri;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * M2.1 + M2.2 + M2.3 — HTTP server on localhost:0 with static files and /api/invoke.
 */
public class JauriHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final int port;
    private final HandlerRegistry registry;
    private static final Gson GSON = new Gson();

    public JauriHttpServer(HandlerRegistry registry) {
        this.registry = registry;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            port = server.getAddress().getPort();

            // M2.3 — POST /api/invoke
            server.createContext("/api/invoke", exchange -> {
                try {
                    if (!"POST".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(405, -1);
                        return;
                    }
                    byte[] body = exchange.getRequestBody().readAllBytes();
                    String json = new String(body, StandardCharsets.UTF_8);
                    JsonObject request = GSON.fromJson(json, JsonObject.class);
                    String cmd = request.get("cmd").getAsString();
                    JsonObject args = request.has("args") ? request.getAsJsonObject("args") : new JsonObject();
                    JsonObject result = registry.handle(cmd, args);
                    byte[] resp = GSON.toJson(result).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length);
                    exchange.getResponseBody().write(resp);
                } catch (Exception e) {
                    JsonObject err = new JsonObject();
                    err.addProperty("error", e.getMessage());
                    byte[] resp = GSON.toJson(err).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, resp.length);
                    exchange.getResponseBody().write(resp);
                } finally {
                    exchange.close();
                }
            });

            // M2.1 — GET /health
            server.createContext("/health", exchange -> {
                JsonObject ok = new JsonObject();
                ok.addProperty("status", "ok");
                byte[] resp = GSON.toJson(ok).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.close();
            });

            server.setExecutor(Executors.newSingleThreadExecutor());

            // M2.2 — Serve static files from resources
            addStaticFileServer();

        } catch (IOException e) {
            throw new RuntimeException("Failed to create HTTP server", e);
        }
    }

    private void addStaticFileServer() {
        java.io.File staticDir = extractStaticResources();
        if (staticDir == null) return;

        server.createContext("/", exchange -> {
            try {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/")) path = "/index.html";
                java.io.File file = new java.io.File(staticDir, path);

                // Security: prevent directory traversal
                String canonicalPath = file.getCanonicalPath();
                String canonicalBase = staticDir.getCanonicalPath();
                if (!canonicalPath.startsWith(canonicalBase)) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }

                if (!file.exists() || !file.isFile()) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                byte[] content = java.nio.file.Files.readAllBytes(file.toPath());
                String mime = getMimeType(path);
                exchange.getResponseHeaders().set("Content-Type", mime);
                exchange.sendResponseHeaders(200, content.length);
                exchange.getResponseBody().write(content);
            } finally {
                exchange.close();
            }
        });
    }

    private java.io.File extractStaticResources() {
        try {
            java.io.File tempDir = java.nio.file.Files.createTempDirectory("jauri-static-").toFile();
            tempDir.deleteOnExit();

            var inputStream = getClass().getClassLoader().getResourceAsStream("static/index.html");
            if (inputStream == null) return null;

            java.io.File staticDir = new java.io.File(tempDir, "static");
            staticDir.mkdirs();
            java.io.File target = new java.io.File(staticDir, "index.html");
            java.nio.file.Files.copy(inputStream, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();

            return staticDir;
        } catch (Exception e) {
            System.err.println("[JauriHttpServer] Failed to extract static resources: " + e.getMessage());
            return null;
        }
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    public void start() {
        server.start();
    }

    public int getPort() {
        return port;
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @Override
    public void close() {
        server.stop(1);
    }
}
