package edu.depauw.dep10.simulator2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.depauw.dep10.simulator2.common.TimerDevice;
import edu.depauw.dep10.simulator2.pep10.Pep10DebugState;
import edu.depauw.dep10.simulator2.pep10.Pep10Machine;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Tests of the countdown timer: counting, expiry, turning it off, and stepping
 * back. The timer is mounted at 0xFF30 on a debug state.
 */
public class TimerDeviceTest {
    private static final Word BASE = Word.of(0xFF30);

    private Pep10DebugState state;
    private TimerDevice timer;

    @BeforeEach
    public void setUp() {
        state = new Pep10Machine().initDebug(TimerDeviceTest::zeroMemory);
        timer = new TimerDevice(BASE);
        state.mount(BASE, timer);
    }

    private static void zeroMemory(MemoryDevice memory) {
        for (int i = 0; i < memory.size(); i++) {
            memory.loadB(Word.of(i), UByte.of(0));
        }
    }

    private int countdown() throws Fault {
        return state.readW(BASE).value();
    }

    private void load(int value) throws Fault {
        state.writeW(BASE, Word.of(value));
    }

    private boolean expired() throws Fault {
        return state.readB(BASE.plus(TimerDevice.STATUS)).bit(0);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            state.tick();
        }
    }

    private void unticks(int n) {
        for (int i = 0; i < n; i++) {
            state.untick();
        }
    }

    @Test
    public void countsDownOnEveryTick() throws Fault {
        load(5);
        assertEquals(5, countdown());
        ticks(3);
        assertEquals(2, countdown());
        assertFalse(expired());
        assertFalse(timer.isExpired());
    }

    @Test
    public void expiresAtZeroAndStaysThere() throws Fault {
        load(3);
        ticks(3);
        assertEquals(0, countdown());
        assertTrue(expired());
        assertTrue(timer.isExpired());
        ticks(10);
        assertEquals(0, countdown());
        assertTrue(expired());
    }

    @Test
    public void loadingZeroTurnsTheTimerOff() throws Fault {
        ticks(100);
        assertEquals(0, countdown());
        assertFalse(expired());
        load(0);
        ticks(100);
        assertFalse(expired());
    }

    @Test
    public void loadingAValueClearsAnExpiry() throws Fault {
        load(2);
        ticks(2);
        assertTrue(expired());
        load(4);
        assertFalse(expired());
        assertEquals(4, countdown());
    }

    @Test
    public void loadingZeroDoesNotCancelAnExpiryThatIsWaiting() throws Fault {
        load(2);
        ticks(2);
        assertTrue(expired());
        load(0);
        assertTrue(expired());
        ticks(5);
        assertTrue(expired());
        load(1);
        assertFalse(expired());
    }

    @Test
    public void loadingOneExpiresAfterOneTick() throws Fault {
        load(1);
        assertFalse(expired());
        ticks(1);
        assertTrue(expired());
    }

    @Test
    public void bytesComposeIntoOneValue() throws Fault {
        state.writeB(BASE, UByte.of(0x01));
        state.writeB(BASE.plus(1), UByte.of(0x2C));
        assertEquals(300, countdown());
        assertEquals(0x01, state.readB(BASE).value());
        assertEquals(0x2C, state.readB(BASE.plus(1)).value());
    }

    @Test
    public void untickPutsTheCountdownBack() throws Fault {
        load(3);
        ticks(3);
        assertTrue(expired());
        unticks(1);
        assertEquals(1, countdown());
        assertFalse(expired());
        unticks(2);
        assertEquals(3, countdown());
        ticks(1);
        assertEquals(2, countdown());
    }

    @Test
    public void untickAfterExpiryStaysConsistent() throws Fault {
        load(2);
        ticks(5); // three ticks after it expired
        unticks(3);
        assertEquals(0, countdown());
        assertTrue(expired());
        unticks(1);
        assertEquals(1, countdown());
        assertFalse(expired());
    }

    @Test
    public void undoingAWriteRestoresTheOldTimer() throws Fault {
        load(10);
        ticks(4);
        state.getStep(); // start a fresh step
        load(50);
        var step = state.getStep();
        assertEquals(50, countdown());
        step.undo(state);
        assertEquals(6, countdown());
        ticks(1);
        assertEquals(5, countdown());
    }

    @Test
    public void undoingAWriteOfZeroRestoresAPendingExpiry() throws Fault {
        load(1);
        ticks(1);
        assertTrue(expired());
        state.getStep();
        load(0);
        var step = state.getStep();
        step.undo(state);
        assertTrue(expired());
    }

    @Test
    public void statusIsReadOnly() {
        assertFalse(timer.canWriteB(BASE.plus(TimerDevice.STATUS)));
        assertTrue(timer.canWriteB(BASE));
        assertTrue(timer.canWriteB(BASE.plus(1)));
    }
}
