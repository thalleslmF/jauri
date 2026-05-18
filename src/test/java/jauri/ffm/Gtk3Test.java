package jauri.ffm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Gtk3Test {

    @Test
    void staticInitDoesNotThrow() {
        // Gtk3 class loading triggers Panama FFM bindings (SymbolLookup + downcallHandle)
        // If any symbol is missing or descriptor is wrong, static init throws
        assertDoesNotThrow(() -> {
            Class<?> c = Gtk3.class;
        });
    }

    @Test
    void constantsDefined() {
        assertEquals(0, Gtk3.GTK_WINDOW_TOPLEVEL);
    }
}
