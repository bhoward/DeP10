package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Represents the registers (byte, word, and individual bit flag) of a
 * particular machine configuration. Internally, each register has an id number
 * for uniform access.
 */
public class RegisterFile {
    /**
     * Selector for a particular byte register with the given id number. The name
     * may be used for visualization or tracing.
     * 
     * @param name
     * @param id
     */
    public static record ByteRegister(String name, int id) {
    }

    /**
     * Selector for a particular word register with the given id number. The name
     * may be used for visualization or tracing.
     * 
     * @param name
     * @param id
     */
    public static record WordRegister(String name, int id) {
    }

    /**
     * Selector for a particular flag within a byte register with the given id
     * number. Position 0 is the low-order bit, and position 7 is the high-order
     * bit. The name may be used for visualization or tracing.
     * 
     * @param name
     * @param id
     * @param position
     */
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
     * Put a byte into the given register.
     * 
     * @param register
     * @param value
     */
    public void put(ByteRegister register, UByte value) {
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
     * Put a word into the given register.
     * 
     * @param register
     * @param value
     */
    public void put(WordRegister register, Word value) {
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
     * Put a flag bit into the given register.
     * 
     * @param flag
     * @param value
     */
    public void put(FlagRegister flag, boolean value) {
        breg[flag.id()] = breg[flag.id()].withBit(flag.position(), value);
    }

    /**
     * Advance the simulation by one time step.
     */
    public void tick() {
        // Do nothing
    }
}
