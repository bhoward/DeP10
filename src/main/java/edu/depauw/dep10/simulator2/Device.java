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

    // The rest of these are for use in debug mode; they have defaults so that
    // devices with no appropriate debug action can still be used in that mode

    /**
     * Reverse the last tick(), if possible.
     */
    default void untick() {
        // Do nothing
    }

    /**
     * Get one byte using "execute" permission from the device at the given address.
     * Any action that might need to be undone will be recorded with the given
     * state. Raises a fault if there is an error.
     * 
     * @param address
     * @param state
     * @return the byte
     * @throws Fault
     */
    default UByte fetchB(Word address, DebugState state) throws Fault {
        return fetchB(address);
    }

    /**
     * Get one byte using "read" permission from the device at the given address.
     * Any action that might need to be undone will be recorded with the given
     * state. Raises a fault if there is an error.
     * 
     * @param address
     * @param state
     * @return the byte
     * @throws Fault
     */
    default UByte readB(Word address, DebugState state) throws Fault {
        return readB(address);
    }

    /**
     * Put one byte using "write" permission to the device at the given address. Any
     * action that might need to be undone will be recorded with the given state.
     * Raises a fault if there is an error.
     * 
     * @param address
     * @param value
     * @param state
     * @throws Fault
     */
    default void writeB(Word address, UByte value, DebugState state) throws Fault {
        writeB(address, value);
    }

    /**
     * Get one word using "execute" permission from the device at the given address.
     * Any action that might need to be undone will be recorded with the given
     * state. Raises a fault if there is an error.
     *
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     *
     * @param address
     * @param state
     * @return the word
     * @throws Fault
     */
    default Word fetchW(Word address, DebugState state) throws Fault {
        var hi = fetchB(address, state);
        var lo = fetchB(address.plus(1), state);
        return hi.with(lo);
    }

    /**
     * Get one word using "read" permission from the device at the given address.
     * Any action that might need to be undone will be recorded with the given
     * state. Raises a fault if there is an error.
     *
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     *
     * @param address
     * @param state
     * @return the word
     * @throws Fault
     */
    default Word readW(Word address, DebugState state) throws Fault {
        var hi = readB(address, state);
        var lo = readB(address.plus(1), state);
        return hi.with(lo);
    }

    /**
     * Put one word using "write" permission to the device at the given address. Any
     * action that might need to be undone will be recorded with the given state.
     * Raises a fault if there is an error.
     * 
     * Override this if there is a distinction between accessing an entire word at
     * this address is different from accessing two adjacent bytes, one at a time.
     *
     * @param address
     * @param value
     * @param state
     * @throws Fault
     */
    default void writeW(Word address, Word value, DebugState state) throws Fault {
        writeB(address, value.hi(), state);
        writeB(address.plus(1), value.lo(), state);
    }
}
