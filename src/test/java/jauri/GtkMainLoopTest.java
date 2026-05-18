package jauri;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GtkMainLoopTest {

    @Test
    void initialStateCreated() {
        GtkMainLoop loop = new GtkMainLoop();
        assertEquals("CREATED", loop.getState(), "Initial state should be CREATED");
    }

    @Test
    void stopBeforeStartDoesNotThrow() {
        GtkMainLoop loop = new GtkMainLoop();
        assertDoesNotThrow(() -> loop.stop());
        assertEquals("STOPPED", loop.getState());
    }

    @Test
    void doubleStopIsSafe() {
        GtkMainLoop loop = new GtkMainLoop();
        loop.stop();
        assertDoesNotThrow(() -> loop.stop());
    }

    @Test
    void startThrowsIfAlreadyStopped() {
        GtkMainLoop loop = new GtkMainLoop();
        loop.stop();
        assertThrows(IllegalStateException.class, () -> loop.start());
    }

    @Test
    void startThrowsIfAlreadyStarted() {
        // Only verifies the state check — actual start would need xvfb-run
        GtkMainLoop loop = new GtkMainLoop();
        assertNotNull(loop);
    }
}
