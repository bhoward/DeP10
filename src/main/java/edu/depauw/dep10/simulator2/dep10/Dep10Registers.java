package edu.depauw.dep10.simulator2.dep10;

import edu.depauw.dep10.simulator2.common.RegisterFile.ByteRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.FlagRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.WordRegister;
import edu.depauw.dep10.simulator2.common.RegisterFileDescriptor;
import edu.depauw.dep10.simulator2.pep10.Pep10Registers;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * "Mixin" interface for the DeP10 register file: everything in Pep/10, plus the
 * high word register H and a second opcode byte for the two-byte (prefixed)
 * instructions.
 */
public interface Dep10Registers extends Pep10Registers {
    // The instruction register is a word: a plain instruction is in the low byte,
    // and a prefixed instruction has its prefix in the high byte, in the same
    // order as the bytes in memory.
    static final WordRegister IR1 = new WordRegister("IR1", 7);

    static final ByteRegister[] BYTE_REGISTERS = { FLAGS };

    static final WordRegister H = new WordRegister("H", 6);

    static final WordRegister[] WORD_REGISTERS = { A, X, H, PC, SP, IR1, IR2, EA };

    static final RegisterFileDescriptor DESCRIPTOR = new RegisterFileDescriptor() {
        @Override
        public ByteRegister[] byteRegisters() {
            return BYTE_REGISTERS;
        }

        @Override
        public WordRegister[] wordRegisters() {
            return WORD_REGISTERS;
        }

        @Override
        public FlagRegister[] flagRegisters() {
            return FLAG_REGISTERS;
        }
    };

    default Word getH() {
        return get(H);
    }

    default void setH(Word value) {
        put(H, value);
    }

    /**
     * @return the whole first instruction word: the opcode in the low byte for a
     *         plain instruction, or the prefix and the second opcode byte for a
     *         prefixed instruction
     */
    default Word getIR1() {
        return get(IR1);
    }

    default void setIR1(Word value) {
        put(IR1, value);
    }

    /**
     * @return the last opcode byte of the current instruction (the second byte of a
     *         prefixed instruction)
     */
    @Override
    default UByte getOpCode() {
        return getIR1().lo();
    }

    @Override
    default void setOpCode(UByte value) {
        setIR1(Word.of(value.value()));
    }

    /**
     * @return the prefix of the current instruction, or zero if it has no prefix
     */
    default UByte getPrefix() {
        return getIR1().hi();
    }
}
