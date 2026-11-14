package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class PlainMemoryDevice implements Device {
    private UByte[] mem;
    
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
