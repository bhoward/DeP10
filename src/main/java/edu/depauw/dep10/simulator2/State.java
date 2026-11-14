package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.simulator2.common.RegisterFile.ByteRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.FlagRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.WordRegister;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Represents all of the mutable state associated with a running machine.
 */
public interface State {
    /**
     * @return true if the machine is currently in the running state
     */
    boolean isRunning();

    /**
     * Put the machine into the running state.
     */
    void run();

    /**
     * Put the machine into the paused state.
     */
    void pause();

    /**
     * Resume running a previously paused machine.
     */
    void resume();

    /**
     * Get a single byte from the given address using "execute" permission. Throws a
     * fault if an error occurs.
     * 
     * @param address
     * @return the byte
     * @throws Fault
     */
    UByte fetchB(Word address) throws Fault;

    /**
     * Get a single byte from the given address using "read" permission. Throws a
     * fault if an error occurs.
     * 
     * @param address
     * @return the byte
     * @throws Fault
     */
    UByte readB(Word address) throws Fault;

    /**
     * Put a single byte into the given address using "write" permission. Throws a
     * fault if an error occurs.
     * 
     * @param address
     * @param value
     * @throws Fault
     */
    void writeB(Word address, UByte value) throws Fault;

    /**
     * Get a word from the given address using "execute" permission. High byte is at
     * the address, and low byte is at the address+1. Throws a fault if an error
     * occurs.
     * 
     * @param address
     * @return the word
     * @throws Fault
     */
    Word fetchW(Word address) throws Fault;

    /**
     * Get a word from the given address using "read" permission. High byte is at
     * the address, and low byte is at the address+1. Throws a fault if an error
     * occurs.
     * 
     * @param address
     * @return the word
     * @throws Fault
     */
    Word readW(Word address) throws Fault;

    /**
     * Put a word into the given address using "write" permission. High byte is at
     * the address, and low byte is at the address+1. Throws a fault if an error
     * occurs.
     * 
     * @param address
     * @param value
     * @throws Fault
     */
    void writeW(Word address, Word value) throws Fault;

    /**
     * Get a byte from the given register.
     * 
     * @param register
     * @return the byte
     */
    UByte get(ByteRegister register);

    /**
     * Put a byte into the given register.
     * 
     * @param register
     * @param value
     */
    void put(ByteRegister register, UByte value);

    /**
     * Get a word from the given register.
     * 
     * @param register
     * @return the word
     */
    Word get(WordRegister register);

    /**
     * Put a word into the given register.
     * 
     * @param register
     * @param value
     */
    void put(WordRegister register, Word value);

    /**
     * Get a flag bit from the given register.
     * 
     * @param flag
     * @return the bit
     */
    boolean get(FlagRegister flag);

    /**
     * Put a flag bit into the given register.
     * 
     * @param flag
     * @param value
     */
    void put(FlagRegister flag, boolean value);
}
