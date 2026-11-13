package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class PlainMemoryDevice implements Device {
    private UByte[] mem;
    
    public PlainMemoryDevice() {
        this.mem = new UByte[65536];
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
