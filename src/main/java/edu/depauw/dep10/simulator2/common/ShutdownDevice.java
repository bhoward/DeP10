package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class ShutdownDevice implements Device {
    private Word baseAddress;
    
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
    public UByte fetchB(Word address, State state) throws Fault {
        return null;
    }

    @Override
    public UByte readB(Word address, State state) throws Fault {
        return null;
    }

    @Override
    public void writeB(Word address, UByte value, State state) throws Fault {
        // TODO should we write-through the value to the underlying memory location?
        state.pause();
    }
}
