package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.DebugDevice;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * A single-byte memory-mapped device that will pause the machine when anything
 * is written to it. It implements DebugDevice because it behaves exactly the
 * same in debug mode; there is no need to implement a debug version of it.
 */
public class ShutdownDevice implements DebugDevice {
    private Word baseAddress;

    /**
     * Construct a ShutdownDevice to be mounted at the given base address.
     * 
     * @param baseAddress
     */
    public ShutdownDevice(Word baseAddress) {
        this.baseAddress = baseAddress;
    }

    @Override
    public boolean canFetchB(Word address) {
        return false;
    }

    @Override
    public boolean canReadB(Word address) {
        return false;
    }

    @Override
    public boolean canWriteB(Word address) {
        return address.equals(baseAddress);
    }

    @Override
    public UByte fetchB(Word address) throws Fault {
        return null;
    }

    @Override
    public UByte readB(Word address) throws Fault {
        return null;
    }

    @Override
    public void writeB(Word address, UByte value) throws Fault {
        throw new Fault("Shutdown");
    }

    @Override
    public void tick() {
        // Do nothing
    }
}
