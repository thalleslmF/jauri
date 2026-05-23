package jauri;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HandlerRegistryTest {

    @Test
    void registerAndHandle() {
        HandlerRegistry r = new HandlerRegistry();
        r.register("hello", args -> {
            JsonObject res = new JsonObject();
            res.addProperty("message", "Hello, " + args.get("name").getAsString());
            return res;
        });
        JsonObject args = new JsonObject();
        args.addProperty("name", "Jauri");
        JsonObject result = r.handle("hello", args);
        assertEquals("Hello, Jauri", result.get("message").getAsString());
    }

    @Test
    void unknownCommandReturnsError() {
        HandlerRegistry r = new HandlerRegistry();
        JsonObject result = r.handle("nope", new JsonObject());
        assertTrue(result.has("error"));
        assertTrue(result.get("error").getAsString().contains("Unknown command"));
    }

    @Test
    void pingRegisteredByDefault() {
        HandlerRegistry r = new HandlerRegistry();
        r.registerDefaults();
        assertTrue(r.hasCommand("ping"));
        JsonObject result = r.handle("ping", new JsonObject());
        assertEquals("pong", result.get("result").getAsString());
    }
}
