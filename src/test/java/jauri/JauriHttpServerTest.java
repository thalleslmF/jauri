package jauri;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class JauriHttpServerTest {

    private HandlerRegistry registry;
    private JauriHttpServer server;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void setup() {
        registry = new HandlerRegistry();
        registry.registerDefaults();
        server = new JauriHttpServer(registry);
        server.start();
    }

    @AfterEach
    void teardown() {
        server.close();
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/health"))
            .GET()
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("ok", json.get("status").getAsString());
    }

    @Test
    void invokePingReturnsPong() throws Exception {
        String json = "{\"cmd\":\"ping\",\"args\":{}}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/api/invoke"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        JsonObject result = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("pong", result.get("result").getAsString());
    }

    @Test
    void invokeUnknownCommandReturnsError() throws Exception {
        String json = "{\"cmd\":\"nope\",\"args\":{}}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/api/invoke"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Unknown command"));
    }

    @Test
    void invokeGetMethodReturns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/api/invoke"))
            .GET()
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, resp.statusCode());
    }

    @Test
    void portIsRandomAndNonZero() {
        assertTrue(server.getPort() > 0, "Port should be > 0");
        assertEquals("http://127.0.0.1:" + server.getPort(), server.getBaseUrl());
    }
}
