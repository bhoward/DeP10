package edu.depauw.dep10.simulator2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.depauw.dep10.simulator2.common.PlainMemoryDevice;
import edu.depauw.dep10.simulator2.common.ProtectedMemoryDevice;
import edu.depauw.dep10.simulator2.common.RegisterFile;
import edu.depauw.dep10.simulator2.pep10.Pep10DebugState;
import edu.depauw.dep10.simulator2.pep10.Pep10Registers;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Tests of the permission overlay on memory: an "rx" range that acts like ROM,
 * an "rw" exception inside it, and stepping back over a write.
 */
public class ProtectedMemoryDeviceTest {
    private static final Word ROM = Word.of(0x0100);
    private static final int ROM_LENGTH = 0x20;
    private static final Word INIT_SP = Word.of(0x0110);

    private PlainMemoryDevice plain;
    private ProtectedMemoryDevice memory;

    @BeforeEach
    public void setUp() {
        plain = new PlainMemoryDevice(0x1000);
        for (int i = 0; i < plain.size(); i++) {
            plain.loadB(Word.of(i), UByte.of(0));
        }
        memory = new ProtectedMemoryDevice(plain);
        memory.protect(ROM, ROM_LENGTH, new Permissions(true, false, true));
        memory.protect(INIT_SP, 2, new Permissions(true, true, false));
    }

    @Test
    public void unprotectedAddressesAllowEverything() throws Fault {
        Word a = Word.of(0x0800);
        assertTrue(memory.canFetchB(a) && memory.canReadB(a) && memory.canWriteB(a));
        memory.writeB(a, UByte.of(7));
        assertEquals(7, memory.readB(a).value());
        assertEquals(7, memory.fetchB(a).value());
    }

    @Test
    public void romCanBeLoadedReadAndFetchedButNotWritten() throws Fault {
        memory.loadB(ROM, UByte.of(0x5A));
        assertEquals(0x5A, memory.readB(ROM).value());
        assertEquals(0x5A, memory.fetchB(ROM).value());
        assertFalse(memory.canWriteB(ROM));
        assertThrows(Fault.class, () -> memory.writeB(ROM, UByte.of(1)));
        assertEquals(0x5A, memory.readB(ROM).value());
    }

    @Test
    public void rwExceptionInsideRomCanBeWrittenButNotFetched() throws Fault {
        memory.writeB(INIT_SP, UByte.of(0x12));
        assertEquals(0x12, memory.readB(INIT_SP).value());
        assertThrows(Fault.class, () -> memory.fetchB(INIT_SP));
        Word next = INIT_SP.plus(2);
        assertThrows(Fault.class, () -> memory.writeB(next, UByte.of(1)));
    }

    @Test
    public void noAccessRangeFaultsOnEverything() {
        Word a = Word.of(0x0200);
        memory.protect(a, 1, Permissions.NONE);
        assertThrows(Fault.class, () -> memory.readB(a));
        assertThrows(Fault.class, () -> memory.fetchB(a));
        assertThrows(Fault.class, () -> memory.writeB(a, UByte.of(1)));
    }

    @Test
    public void rangeThatDoesNotFitIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> memory.protect(Word.of(0x0FFF), 2, Permissions.NONE));
    }

    @Test
    public void wordAccessChecksBothBytes() throws Fault {
        assertThrows(Fault.class, () -> memory.writeW(ROM.plus(ROM_LENGTH - 1), Word.of(0x1234)));
        memory.writeW(INIT_SP, Word.of(0xBEEF));
        assertEquals(0xBEEF, memory.readW(INIT_SP).value());
    }

    @Test
    public void steppingBackUndoesAWriteToTheRwException() throws Fault {
        var debug = new Pep10DebugState(memory, new RegisterFile(Pep10Registers.DESCRIPTOR));
        debug.getStep(); // start a fresh step
        debug.writeB(INIT_SP, UByte.of(0x77));
        var step = debug.getStep();
        assertEquals(0x77, debug.readB(INIT_SP).value());
        step.undo(debug);
        assertEquals(0, debug.readB(INIT_SP).value());
    }

    @Test
    public void debugWriteToRomFaultsAndRecordsNothing() throws Fault {
        var debug = new Pep10DebugState(memory, new RegisterFile(Pep10Registers.DESCRIPTOR));
        assertThrows(Fault.class, () -> debug.writeB(ROM, UByte.of(1)));
        assertEquals(0, debug.readB(ROM).value());
    }
}
