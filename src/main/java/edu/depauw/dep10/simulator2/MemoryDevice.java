package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Represents a device accepting addresses from 0 to size - 1, into which the
 * simulator is allowed to load data before running.
 */
public interface MemoryDevice extends Device {
    /**
     * @return one past the largest valid memory address in this device
     */
    int size();

    /**
     * Load the given byte at the specified address, without checking permissions.
     * 
     * @param address
     * @param value
     */
    void loadB(Word address, UByte value);
}
