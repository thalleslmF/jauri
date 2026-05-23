package jauri;

import com.google.gson.JsonObject;

/**
 * M2.4 — Registry mapping command names to handler functions.
 */
public class HandlerRegistry {

    private final java.util.Map<String, Handler> handlers = new java.util.concurrent.ConcurrentHashMap<>();

    @FunctionalInterface
    public interface Handler {
        JsonObject handle(JsonObject args);
    }

    public void register(String cmd, Handler handler) {
        handlers.put(cmd, handler);
    }

    public JsonObject handle(String cmd, JsonObject args) {
        Handler h = handlers.get(cmd);
        if (h == null) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "Unknown command: " + cmd);
            return err;
        }
        return h.handle(args);
    }

    public boolean hasCommand(String cmd) {
        return handlers.containsKey(cmd);
    }

    /** Register the built-in "ping" command. */
    public void registerDefaults() {
        register("ping", args -> {
            JsonObject result = new JsonObject();
            result.addProperty("result", "pong");
            return result;
        });
    }
}
