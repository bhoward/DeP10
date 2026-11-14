package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * A memory-mapped hardware device that can be mounted over certain memory
 * locations in a {@link Machine}.
 */
public interface Device {
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
     * If this is a device to be used in debug mode, then any action that might need
     * to be undone will be recorded with the given state. Raises a fault if there
     * is an error.
     * 
     * @param address
     * @param state
     * @return the byte
     * @throws Fault
     */
    UByte fetchB(Word address, State state) throws Fault;

    /**
     * Get one byte using "read" permission from the device at the given address. If
     * this is a device to be used in debug mode, then any action that might need to
     * be undone will be recorded with the given state. Raises a fault if there is
     * an error.
     * 
     * @param address
     * @param state
     * @return the byte
     * @throws Fault
     */
    UByte readB(Word address, State state) throws Fault;

    /**
     * Put one byte using "write" permission to the device at the given address. If
     * this is a device to be used in debug mode, then any action that might need to
     * be undone will be recorded with the given state. Raises a fault if there is
     * an error.
     * 
     * @param address
     * @param value
     * @param state
     * @throws Fault
     */
    void writeB(Word address, UByte value, State state) throws Fault;

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
     * If this is a device to be used in debug mode, then any action that might need
     * to be undone will be recorded with the given state. Raises a fault if there
     * is an error.
     *
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     *
     * @param address
     * @param state
     * @return the word
     * @throws Fault
     */
    default Word fetchW(Word address, State state) throws Fault {
        var hi = fetchB(address, state);
        var lo = fetchB(address.plus(1), state);
        return hi.with(lo);
    }

    /**
     * Get one word using "read" permission from the device at the given address. If
     * this is a device to be used in debug mode, then any action that might need to
     * be undone will be recorded with the given state. Raises a fault if there is
     * an error.
     *
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     *
     * @param address
     * @param state
     * @return the word
     * @throws Fault
     */
    default Word readW(Word address, State state) throws Fault {
        var hi = readB(address, state);
        var lo = readB(address.plus(1), state);
        return hi.with(lo);
    }

    /**
     * Put one word using "write" permission to the device at the given address. If
     * this is a device to be used in debug mode, then any action that might need to
     * be undone will be recorded with the given state. Raises a fault if there is
     * an error.
     * 
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     *
     * @param address
     * @param value
     * @param state
     * @throws Fault
     */
    default void writeW(Word address, Word value, State state) throws Fault {
        writeB(address, value.hi(), state);
        writeB(address.plus(1), value.lo(), state);
    }
}
