package jauri.ffm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LibCTest {

    @Test
    void getpidReturnsPositive() {
        int pid = LibC.getpid();
        assertTrue(pid > 0, "PID must be positive, got: " + pid);
    }
}
