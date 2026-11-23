package edu.depauw.dep10.simulator2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import edu.depauw.dep10.simulator2.pep10.Pep10Image;
import edu.depauw.dep10.simulator2.pep10.Pep10Machine;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Tests that the permissions in an image's segment headers are enforced by the
 * Pep/10 machine, so that the operating system can sit in "ROM": the system code
 * is rx, the initial stack pointer is rw, and an image with no flags is fully
 * writable, as before.
 */
public class Pep10ProtectionTest {
    private static Image zeroThen(String object) {
        var image = new Pep10Image();
        image.addString(object);
        return memory -> {
            for (int i = 0; i < memory.size(); i++) {
                memory.loadB(Word.of(i), UByte.of(0));
            }
            image.loadInto(memory);
        };
    }

    private static Word w(int address) {
        return Word.of(address);
    }

    /** A small stand-in for the OS layout: rwz data, rx code, rw initial SP. */
    private static final String OS_LIKE = "[FACA rwz] 00 00 00 [FB52 rx] 11 22 33 44 [FFFB rw] FB 8F";

    @Test
    public void systemCodeCanBeReadAndFetchedButNotWritten() throws Fault {
        var state = new Pep10Machine().init(zeroThen(OS_LIKE));
        assertEquals(0x11, state.readB(w(0xFB52)).value());
        assertEquals(0x11, state.fetchB(w(0xFB52)).value());
        assertThrows(Fault.class, () -> state.writeB(w(0xFB52), UByte.of(1)));
        assertEquals(0x11, state.readB(w(0xFB52)).value());
    }

    @Test
    public void wordWriteIntoSystemCodeFaults() {
        var state = new Pep10Machine().init(zeroThen(OS_LIKE));
        assertThrows(Fault.class, () -> state.writeW(w(0xFB53), Word.of(0x1234)));
    }

    @Test
    public void initialStackPointerIsReadWrite() throws Fault {
        var state = new Pep10Machine().init(zeroThen(OS_LIKE));
        assertEquals(0xFB8F, state.readW(w(0xFFFB)).value());
        assertDoesNotThrow(() -> state.writeW(w(0xFFFB), Word.of(0x1234)));
        assertEquals(0x1234, state.readW(w(0xFFFB)).value());
    }

    @Test
    public void rwDataCanBeWrittenButNotExecuted() throws Fault {
        var state = new Pep10Machine().init(zeroThen(OS_LIKE));
        assertDoesNotThrow(() -> state.writeB(w(0xFACA), UByte.of(9)));
        assertEquals(9, state.readB(w(0xFACA)).value());
        assertThrows(Fault.class, () -> state.fetchB(w(0xFACA)));
    }

    @Test
    public void imageWithNoFlagsIsFullyWritable() throws Fault {
        var state = new Pep10Machine().init(zeroThen("[0100] 11 22 33"));
        assertDoesNotThrow(() -> state.writeB(w(0x0100), UByte.of(5)));
        assertEquals(5, state.readB(w(0x0100)).value());
        assertEquals(5, state.fetchB(w(0x0100)).value());
        assertDoesNotThrow(() -> state.writeB(w(0xFB52), UByte.of(5)));
    }

    @Test
    public void debugMachineEnforcesTheSamePermissions() throws Fault {
        var state = new Pep10Machine().initDebug(zeroThen(OS_LIKE));
        assertThrows(Fault.class, () -> state.writeB(w(0xFB52), UByte.of(1)));
        assertEquals(0x11, state.readB(w(0xFB52)).value());
        state.getStep(); // start a fresh step
        state.writeW(w(0xFFFB), Word.of(0x4321));
        var step = state.getStep();
        step.undo(state);
        assertEquals(0xFB8F, state.readW(w(0xFFFB)).value());
    }

    @Test
    public void segmentThatWrapsAroundIsProtectedOnBothEnds() {
        // Five bytes starting two before the end of memory wrap to 0000 to 0002
        var state = new Pep10Machine().init(zeroThen("[FFFE rx] 01 02 03 04 05"));
        assertThrows(Fault.class, () -> state.writeB(w(0xFFFE), UByte.of(9)));
        assertThrows(Fault.class, () -> state.writeB(w(0x0002), UByte.of(9)));
        assertDoesNotThrow(() -> state.writeB(w(0x0003), UByte.of(9)));
    }
}
