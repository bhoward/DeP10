package edu.depauw.dep10.simulator2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.depauw.dep10.simulator2.dep10.Dep10DebugState;
import edu.depauw.dep10.simulator2.dep10.Dep10Machine;
import edu.depauw.dep10.simulator2.dep10.Dep10State;
import edu.depauw.dep10.simulator2.dep10.Dep10Table;
import edu.depauw.dep10.simulator2.dep10.ExtensibleTable;
import edu.depauw.dep10.simulator2.pep10.Pep10Image;
import edu.depauw.dep10.simulator2.pep10.Pep10Table;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Tests of the DeP10 machine: the H register, prefixed instruction fetch, the
 * utility extension instructions, and the clock.
 */
public class Dep10MachineTest {
    private static final String VECTORS = "[FFF9] 00 00 FB 8F"; // initial PC = 0000, initial SP = FB8F

    private final Dep10Machine machine = new Dep10Machine();

    /** An image with the given object code, and zeros everywhere else. */
    private static Image image(String object) {
        var image = new Pep10Image();
        image.addString(object);
        return memory -> {
            for (int i = 0; i < memory.size(); i++) {
                memory.loadB(Word.of(i), UByte.of(0));
            }
            image.loadInto(memory);
        };
    }

    private Dep10State start(String program) {
        return machine.init(image(program + " " + VECTORS));
    }

    private Dep10DebugState startDebug(String program) {
        return machine.initDebug(image(program + " " + VECTORS));
    }

    private void step(State state) throws Fault {
        machine.fetch(state).execute(state);
    }

    private static Word w(int value) {
        return Word.of(value);
    }

    @Test
    public void registersStartFromTheVectors() {
        var state = start("");
        assertEquals(0x0000, state.getPC().value());
        assertEquals(0xFB8F, state.getSP().value());
        assertEquals(0, state.getH().value());
    }

    @Test
    public void prefixedFetchReadsBothBytesAndRecordsThem() throws Fault {
        var state = start("09 02"); // prefix 09, SWAPHA
        var op = machine.fetch(state);
        assertEquals("SWAPHA", op.getMnemonic());
        assertEquals(2, state.getPC().value());
        assertEquals(0x0902, state.getIR1().value()); // the prefix is the high byte
        assertEquals(0x09, state.getPrefix().value());
        assertEquals(0x02, state.getOpCode().value());
    }

    @Test
    public void unprefixedFetchClearsThePrefix() throws Fault {
        var state = start("09 02 C0 00 05"); // SWAPHA, then LDWA 5,i
        step(state);
        assertEquals(0x0902, state.getIR1().value());
        step(state);
        assertEquals(0x00C0, state.getIR1().value());
        assertEquals(0x00, state.getPrefix().value());
        assertEquals(5, state.getA().value());
    }

    @Test
    public void baseInstructionsStillWork() throws Fault {
        var state = start("C0 12 34"); // LDWA 0x1234,i
        step(state);
        assertEquals(0x1234, state.getA().value());
        assertEquals(3, state.getPC().value());
    }

    @Test
    public void swapHAExchangesHAndA() throws Fault {
        var state = start("09 02");
        state.setA(w(1));
        state.setH(w(2));
        step(state);
        assertEquals(2, state.getA().value());
        assertEquals(1, state.getH().value());
    }

    @Test
    public void swapHXExchangesHAndX() throws Fault {
        var state = start("09 03");
        state.setX(w(3));
        state.setH(w(4));
        step(state);
        assertEquals(4, state.getX().value());
        assertEquals(3, state.getH().value());
    }

    @Test
    public void swapAXExchangesAAndX() throws Fault {
        var state = start("09 01");
        state.setA(w(5));
        state.setX(w(6));
        step(state);
        assertEquals(6, state.getA().value());
        assertEquals(5, state.getX().value());
    }

    @Test
    public void pushAndPopMoveTheStackPointer() throws Fault {
        var state = start("09 04 09 07"); // PUSHA, POPX
        state.setA(w(0xBEEF));
        step(state);
        assertEquals(0xFB8D, state.getSP().value());
        assertEquals(0xBEEF, state.readW(w(0xFB8D)).value());
        step(state);
        assertEquals(0xFB8F, state.getSP().value());
        assertEquals(0xBEEF, state.getX().value());
    }

    @Test
    public void pushXAndPopA() throws Fault {
        var state = start("09 05 09 06"); // PUSHX, POPA
        state.setX(w(0x1234));
        step(state);
        step(state);
        assertEquals(0x1234, state.getA().value());
    }

    @Test
    public void leaaLoadsTheAddressWithoutReadingMemory() throws Fault {
        var state = start("09 11 80 00"); // LEAA 0x8000,d
        step(state);
        assertEquals(0x8000, state.getA().value());
        assertTrue(state.getN());
        assertFalse(state.getZ());
    }

    @Test
    public void leaxIndexedUsesX() throws Fault {
        var state = start("09 1D 01 00"); // LEAX 0x0100,x (code 24 + 5)
        state.setX(w(0x0010));
        step(state);
        assertEquals(0x0110, state.getX().value());
    }

    @Test
    public void leaaImmediateIsNotAllowed() {
        var state = start("09 10 00 00"); // code 16 would be mode i, which is not installed
        assertThrows(Fault.class, () -> step(state));
    }

    @Test
    public void swapaExchangesAWithMemory() throws Fault {
        var state = start("09 21 01 00 [0100] BE EF"); // SWAPA 0x0100,d
        state.setA(w(1));
        step(state);
        assertEquals(0xBEEF, state.getA().value());
        assertEquals(1, state.readW(w(0x0100)).value());
        assertTrue(state.getN());
    }

    @Test
    public void swapxExchangesXWithMemory() throws Fault {
        var state = start("09 29 01 00 [0100] 00 07"); // SWAPX 0x0100,d (code 40 + 1)
        state.setX(w(9));
        step(state);
        assertEquals(7, state.getX().value());
        assertEquals(9, state.readW(w(0x0100)).value());
        assertFalse(state.getZ());
    }

    @Test
    public void brpBranchesOnlyWhenPIsSet() throws Fault {
        var taken = start("09 30 AB CD"); // BRP 0xABCD,i
        taken.setFlags(UByte.of(0x80));
        step(taken);
        assertEquals(0xABCD, taken.getPC().value());

        var notTaken = start("09 30 AB CD");
        step(notTaken);
        assertEquals(4, notTaken.getPC().value());
    }

    @Test
    public void unimplementedExtensionFaults() {
        var state = start("09 7F");
        assertThrows(Fault.class, () -> step(state));
    }

    @Test
    public void arithmeticPrefixIsNotInstalledYet() {
        var state = start("08 11");
        assertThrows(Fault.class, () -> step(state));
    }

    @Test
    public void clockIsMountedAtFF00() throws Fault {
        var state = start("");
        for (int i = 0; i < 5; i++) {
            state.tick();
        }
        assertEquals(5, state.readW(Dep10Machine.CLOCK.plus(6)).value());
    }

    @Test
    public void steppingBackUndoesRegisterAndMemoryChanges() throws Fault {
        var state = startDebug("09 04"); // PUSHA
        state.setA(w(0x4321));
        state.getStep(); // start a fresh step
        step(state);
        var step = state.getStep();
        assertEquals(0xFB8D, state.getSP().value());
        step.undo(state);
        assertEquals(0xFB8F, state.getSP().value());
        assertEquals(0, state.readW(w(0xFB8D)).value());
        assertEquals(0, state.getPC().value());
        assertEquals(0, state.getIR1().value());
    }

    @Test
    public void lookupOfPrefixedInstructionsPutsThePrefixInTheHighByte() {
        var table = new ExtensibleTable(Pep10Table.UNIMPLEMENTED);
        Dep10Table.installDep10(table);
        assertEquals(0x0902, table.lookup("SWAPHA", ""));
        assertEquals(0x0911, table.lookup("LEAA", "d"));
        assertEquals(0x0930, table.lookup("BRP", "i"));
        assertEquals(0x0931, table.lookup("BRP", "x"));
        assertEquals(0xC0, table.lookup("LDWA", "i"));
        assertEquals(-1, table.lookup("LEAA", "i"));
    }
}
