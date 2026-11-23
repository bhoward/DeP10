package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Represents a device accepting addresses from 0 to size - 1, into which the
 * simulator is allowed to load data before (or while) running.
 */
public interface MemoryDevice extends Device {
    /**
     * @return one past the largest valid memory address in this device
     */
    int size();

    /**
     * Load the given byte at the specified address, without checking permissions or
     * recording debug actions.
     * 
     * @param address
     * @param value
     */
    void loadB(Word address, UByte value);

    /**
     * Record what user mode may do with a range of addresses, as read from an
     * image. A device with no protection ignores this, which is what this default
     * does.
     * 
     * @param start  the first address in the range
     * @param length the number of bytes in the range
     * @param what   what is allowed in the range
     */
    default void protect(Word start, int length, Permissions what) {
        // Do nothing
    }
}
