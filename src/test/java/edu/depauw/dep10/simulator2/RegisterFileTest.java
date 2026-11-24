package edu.depauw.dep10.simulator2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import edu.depauw.dep10.simulator2.common.RegisterFile;
import edu.depauw.dep10.simulator2.pep10.Pep10Registers;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Tests of the initial contents of a register file, and of copying one.
 */
public class RegisterFileTest {
    @Test
    public void everyRegisterStartsAtZero() {
        var file = new RegisterFile(Pep10Registers.DESCRIPTOR);
        for (var register : Pep10Registers.BYTE_REGISTERS) {
            assertEquals(0, file.get(register).value());
        }
        for (var register : Pep10Registers.WORD_REGISTERS) {
            assertEquals(0, file.get(register).value());
        }
        for (var flag : Pep10Registers.FLAG_REGISTERS) {
            assertFalse(file.get(flag));
        }
    }

    @Test
    public void flagCanBeSetBeforeAnythingIsStored() {
        var file = new RegisterFile(Pep10Registers.DESCRIPTOR);
        file.put(Pep10Registers.Z, true);
        assertEquals(true, file.get(Pep10Registers.Z));
        assertFalse(file.get(Pep10Registers.N));
    }

    @Test
    public void copyKeepsTheDescriptorSoItCanBeDisplayed() {
        var file = new RegisterFile(Pep10Registers.DESCRIPTOR);
        file.put(Pep10Registers.A, Word.of(0x1234));
        var copy = file.copy();
        assertEquals(file.display(), copy.display());
    }

    @Test
    public void copyIsIndependentOfTheOriginal() {
        var file = new RegisterFile(Pep10Registers.DESCRIPTOR);
        var copy = file.copy();
        file.put(Pep10Registers.A, Word.of(7));
        file.put(Pep10Registers.FLAGS, UByte.of(1));
        assertEquals(0, copy.get(Pep10Registers.A).value());
        assertEquals(0, copy.get(Pep10Registers.FLAGS).value());
    }
}
