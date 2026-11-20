package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * A memory-mapped hardware device that can be mounted over certain memory
 * locations in a {@link Machine}.
 */
public interface Device {
    /**
     * Advance the simulation by one time step.
     */
    void tick();

    /**
     * @param address
     * @return true if the device has an executable byte at the given address
     */
    boolean canFetchB(Word address);

    /**
     * @param address
     * @return true if the device has a readable byte at the given address
     */
    boolean canReadB(Word address);

    /**
     * @param address
     * @return true if the device allows a byte to be written to the given address
     */
    boolean canWriteB(Word address);

    /**
     * Get one byte using "execute" permission from the device at the given address.
     * Raises a fault if there is an error.
     * 
     * @param address
     * @return the byte
     * @throws Fault
     */
    UByte fetchB(Word address) throws Fault;

    /**
     * Get one byte using "read" permission from the device at the given address.
     * Raises a fault if there is an error.
     * 
     * @param address
     * @return the byte
     * @throws Fault
     */
    UByte readB(Word address) throws Fault;

    /**
     * Put one byte using "write" permission to the device at the given address.
     * Raises a fault if there is an error.
     * 
     * @param address
     * @param value
     * @throws Fault
     */
    void writeB(Word address, UByte value) throws Fault;

    /**
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     * 
     * @param address
     * @return true if the device has an executable word at the given address
     */
    default boolean canFetchW(Word address) {
        return canFetchB(address) && canFetchB(address.plus(1));
    }

    /**
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     * 
     * @param address
     * @return true if the device has a readable word at the given address
     */
    default boolean canReadW(Word address) {
        return canReadB(address) && canReadB(address.plus(1));
    }

    /**
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     * 
     * @param address
     * @return true if the device allows a word to be written to the given address
     */
    default boolean canWriteW(Word address) {
        return canWriteB(address) && canWriteB(address.plus(1));
    }

    /**
     * Get one word using "execute" permission from the device at the given address.
     * Raises a fault if there is an error.
     *
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     *
     * @param address
     * @return the word
     * @throws Fault
     */
    default Word fetchW(Word address) throws Fault {
        var hi = fetchB(address);
        var lo = fetchB(address.plus(1));
        return hi.with(lo);
    }

    /**
     * Get one word using "read" permission from the device at the given address.
     * Raises a fault if there is an error.
     *
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     *
     * @param address
     * @return the word
     * @throws Fault
     */
    default Word readW(Word address) throws Fault {
        var hi = readB(address);
        var lo = readB(address.plus(1));
        return hi.with(lo);
    }

    /**
     * Put one word using "write" permission to the device at the given address.
     * Raises a fault if there is an error.
     * 
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     *
     * @param address
     * @param value
     * @throws Fault
     */
    default void writeW(Word address, Word value) throws Fault {
        writeB(address, value.hi());
        writeB(address.plus(1), value.lo());
    }
}
