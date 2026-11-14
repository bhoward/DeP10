package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Represents the registers (byte, word, and individual bit flag) of a
 * particular machine configuration. Internally, each register has an id number
 * for uniform access.
 */
public class RegisterFile {
    public static record ByteRegister(String name, int id) {
    }

    public static record WordRegister(String name, int id) {
    }

    public static record FlagRegister(String name, int id, int position) {
    }

    private UByte[] breg;
    private Word[] wreg;

    /**
     * Construct a RegisterFile with the given number of byte and word registers.
     * Any flags should be part of one of the byte registers, so they do not need to
     * be accounted for separately.
     * 
     * @param numByteRegisters
     * @param numWordRegisters
     */
    public RegisterFile(int numByteRegisters, int numWordRegisters) {
        this.breg = new UByte[numByteRegisters];
        this.wreg = new Word[numWordRegisters];
    }

    /**
     * Get a byte from the given register.
     * 
     * @param register
     * @return the byte
     */
    public UByte get(ByteRegister register) {
        return breg[register.id()];
    }

    /**
     * Put a byte into the given register. When this is overridden by a debug
     * version, any action that might need to be undone will be recorded with the
     * given state.
     * 
     * @param register
     * @param value
     * @param state
     */
    public void put(ByteRegister register, UByte value, State state) {
        breg[register.id()] = value;
    }

    /**
     * Get a word from the given register.
     * 
     * @param register
     * @return the word
     */
    public Word get(WordRegister register) {
        return wreg[register.id()];
    }

    /**
     * Put a word into the given register. When this is overridden by a debug
     * version, any action that might need to be undone will be recorded with the
     * given state.
     * 
     * @param register
     * @param value
     * @param state
     */
    public void put(WordRegister register, Word value, State state) {
        wreg[register.id()] = value;
    }

    /**
     * Get a flag bit from the given register.
     * 
     * @param flag
     * @return the bit
     */
    public boolean get(FlagRegister flag) {
        return breg[flag.id()].bit(flag.position());
    }

    /**
     * Put a flag bit into the given register. When this is overridden by a debug
     * version, any action that might need to be undone will be recorded with the
     * given state.
     * 
     * @param flag
     * @param value
     * @param state
     */
    public void put(FlagRegister flag, boolean value, State state) {
        breg[flag.id()] = breg[flag.id()].withBit(flag.position(), value);
    }
}
