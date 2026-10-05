package dev.ironsnouveau.casting;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ChantHudTimerTest {
    @Test void countsDownThenExpiresWithoutAnEndPacket() {
        var timer = new ChantHudTimer();
        var token = UUID.randomUUID();
        timer.update(token, 80, 80, 100);
        assertTrue(timer.active(100));
        assertEquals(.25f, timer.progress(120));
        timer.update(token, 80, 50, 125); // Server correction after drift/latency.
        assertEquals(40, timer.remaining(135));
        assertEquals(.5f, timer.progress(135));
        assertFalse(timer.active(175));
        assertEquals(1f, timer.progress(180));
    }
    @Test void oldCancellationCannotClearReplacementAndWorldChangeClearsState() {
        var timer = new ChantHudTimer();
        var old = UUID.randomUUID(); var current = UUID.randomUUID();
        timer.update(old, 40, 40, 0);
        timer.update(current, 100, 100, 2);
        timer.update(old, 40, 0, 3);
        assertTrue(timer.active(3));
        assertEquals(100, timer.duration());
        timer.update(current, 100, 0, 4);
        assertFalse(timer.active(4));
        timer.update(current, 100, 100, 5);
        timer.clear();
        assertFalse(timer.active(5));
        assertEquals(0, timer.duration());
    }
}
