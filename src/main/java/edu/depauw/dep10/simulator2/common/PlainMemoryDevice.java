package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * A simple memory device that just holds an array of bytes. Although it takes a
 * size in its constructor, it does not do any checking that the addresses are
 * in range.
 */
public class PlainMemoryDevice implements Device {
    private UByte[] mem;

    /**
     * Construct a memory device containing the given number of bytes.
     * 
     * @param size
     */
    public PlainMemoryDevice(int size) {
        this.mem = new UByte[size];
    }

    @Override
    public boolean canFetchB(Word address) {
        return true;
    }

    @Override
    public boolean canReadB(Word address) {
        return true;
    }

    @Override
    public boolean canWriteB(Word address) {
        return true;
    }

    @Override
    public UByte fetchB(Word address, State state) throws Fault {
        return mem[address.value()];
    }

    @Override
    public UByte readB(Word address, State state) throws Fault {
        return mem[address.value()];
    }

    @Override
    public void writeB(Word address, UByte value, State state) throws Fault {
        mem[address.value()] = value;
    }
}
