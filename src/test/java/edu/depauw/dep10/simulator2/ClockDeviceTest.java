package edu.depauw.dep10.simulator2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.depauw.dep10.simulator2.common.ClockDevice;
import edu.depauw.dep10.simulator2.pep10.Pep10DebugState;
import edu.depauw.dep10.simulator2.pep10.Pep10Machine;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Tests of the real time clock: the counter, the latch, the compare status, and
 * stepping back with untick. The clock is mounted at 0xFFC0 on a debug state.
 */
public class ClockDeviceTest {
    private static final Word BASE = Word.of(0xFFC0);

    private Pep10DebugState state;
    private ClockDevice clock;

    @BeforeEach
    public void setUp() {
        state = new Pep10Machine().initDebug(ClockDeviceTest::zeroMemory);
        clock = new ClockDevice(BASE);
        state.mount(clock);
    }

    private static void zeroMemory(MemoryDevice memory) {
        for (int i = 0; i < memory.size(); i++) {
            memory.loadB(Word.of(i), UByte.of(0));
        }
    }

    private Word word(int offset) throws Fault {
        return state.readW(BASE.plus(offset));
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

    private void setCompare(int value) throws Fault {
        state.writeW(BASE.plus(ClockDevice.COMPARE), Word.of(value >>> 16));
        state.writeW(BASE.plus(ClockDevice.COMPARE + 2), Word.of(value & 0xFFFF));
    }

    private void setControl(int value) throws Fault {
        state.writeB(BASE.plus(ClockDevice.CONTROL), UByte.of(value));
    }

    private boolean status() throws Fault {
        return state.readB(BASE.plus(ClockDevice.STATUS)).bit(0);
    }

    @Test
    public void counterCountsTicks() throws Fault {
        assertEquals(0, word(6).value());
        ticks(5);
        assertEquals(0, word(0).value());
        assertEquals(0, word(2).value());
        assertEquals(0, word(4).value());
        assertEquals(5, word(6).value());
    }

    @Test
    public void latchGivesOneConsistentValueWhenReadInOrder() throws Fault {
        ticks(0xFFFF);
        var w0 = word(0); // latches the counter
        var w1 = word(2);
        var w2 = word(4);
        ticks(1); // the live counter is now 0x10000
        var w3 = word(6); // still read from the latch
        assertEquals(0, w0.value() | w1.value() | w2.value());
        assertEquals(0xFFFF, w3.value());
    }

    @Test
    public void readingOutOfOrderCanGiveATornValue() throws Fault {
        ticks(0xFFFF);
        var high = word(4); // no latch: live value 0x0000
        ticks(1);
        var low = word(6); // live value is now 0x10000, so the low word is 0
        // The true counter was 0xFFFF and then 0x10000; the program saw 0
        assertEquals(0, high.value());
        assertEquals(0, low.value());
        assertEquals(0x10000, clock.getTicks());
    }

    @Test
    public void statusIsSetOnlyWhenEnabledAndCompareReached() throws Fault {
        setCompare(3);
        assertFalse(status()); // not enabled
        setControl(1);
        assertFalse(status());
        ticks(2);
        assertFalse(status());
        ticks(1);
        assertTrue(status());
        ticks(5);
        assertTrue(status());
        setControl(0);
        assertFalse(status());
    }

    @Test
    public void compareAndControlReadBack() throws Fault {
        setCompare(0x12345678);
        setControl(0xFF);
        assertEquals(0x1234, word(ClockDevice.COMPARE).value());
        assertEquals(0x5678, word(ClockDevice.COMPARE + 2).value());
        assertEquals(0x03, state.readB(BASE.plus(ClockDevice.CONTROL)).value()); // only two bits are kept
    }

    @Test
    public void untickGoesBackOneTickEach() throws Fault {
        setCompare(4);
        setControl(1);
        ticks(5);
        assertEquals(5, word(6).value());
        assertTrue(status());
        unticks(1);
        assertEquals(4, word(6).value());
        assertTrue(status());
        unticks(1);
        assertEquals(3, word(6).value());
        assertFalse(status());
        unticks(3);
        assertEquals(0, word(6).value());
        unticks(2); // cannot go below zero
        assertEquals(0, word(6).value());
    }

    @Test
    public void untickClearsTheLatch() throws Fault {
        ticks(3);
        word(0); // latch holds 3
        ticks(2);
        unticks(1); // live counter is 4, latch cleared
        // reading the low word without reading the high word first uses the live value
        assertEquals(4, word(6).value());
    }

    @Test
    public void undoingAStepRestoresCompareAndControl() throws Fault {
        state.getStep(); // start a fresh step
        setCompare(7);
        setControl(1);
        var step = state.getStep();
        assertEquals(7, word(ClockDevice.COMPARE + 2).value());
        step.undo(state);
        assertEquals(0, word(ClockDevice.COMPARE + 2).value());
        assertEquals(0, state.readB(BASE.plus(ClockDevice.CONTROL)).value());
    }

    @Test
    public void counterAndStatusAreReadOnly() {
        assertFalse(clock.canWriteB(BASE.plus(ClockDevice.COUNTER)));
        assertFalse(clock.canWriteB(BASE.plus(7)));
        assertFalse(clock.canWriteB(BASE.plus(ClockDevice.STATUS)));
        assertTrue(clock.canWriteB(BASE.plus(ClockDevice.COMPARE)));
        assertTrue(clock.canWriteB(BASE.plus(ClockDevice.CONTROL)));
    }

    @Test
    public void stateTickReachesTheClock() {
        ticks(10);
        assertEquals(10, clock.getTicks());
        unticks(4);
        assertEquals(6, clock.getTicks());
    }
}
