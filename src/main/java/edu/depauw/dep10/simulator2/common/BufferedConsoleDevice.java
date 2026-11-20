package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * An I/O device that maintains buffers of the total input and output of a run
 * of the program, allowing single-stepping backwards and forwards.
 */
public class BufferedConsoleDevice implements Device {
    private Word base;

    public BufferedConsoleDevice(Word baseAddress) {
        this.base = baseAddress;
    }

    @Override
    public void tick() {
        // Do nothing
    }

    @Override
    public boolean canFetchB(Word address) {
        return false;
    }

    @Override
    public boolean canReadB(Word address) {
        return address.equals(base);
    }

    @Override
    public boolean canWriteB(Word address) {
        return address.equals(base.plus(1));
    }

    @Override
    public UByte fetchB(Word address) throws Fault {
        return null;
    }

    @Override
    public UByte readB(Word address) throws Fault {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeB(Word address, UByte value) throws Fault {
        // TODO Auto-generated method stub

    }
}
