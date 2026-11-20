package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * A {@link Device} that can be used in debug mode to record actions for tracing
 * or stepping backwards. All of the methods have defaults that do nothing, or
 * delegate to the non-debug versions (which will not record any actions).
 */
public interface DebugDevice extends Device {
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
