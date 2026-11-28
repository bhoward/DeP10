package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Action;
import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.MemoryDevice;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * A simple memory device that just holds an array of bytes. Although it takes a
 * size in its constructor, it does not do any checking that the addresses are
 * in range.
 */
public class PlainMemoryDevice implements MemoryDevice {
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
    public UByte fetchB(Word address) throws Fault {
        return mem[address.value()];
    }

    @Override
    public UByte readB(Word address) throws Fault {
        return mem[address.value()];
    }

    @Override
    public void writeB(Word address, UByte value) throws Fault {
        mem[address.value()] = value;
    }

    @Override
    public int size() {
        return mem.length;
    }

    @Override
    public void loadB(Word address, UByte value) {
        mem[address.value()] = value;
    }

    @Override
    public void tick() {
        // Do nothing
    }
    
    // These are for use in debug mode
    
    private record PlainMemoryWriteAction(MemoryDevice parent, Word address, UByte newValue, UByte oldValue) implements Action {
        @Override
        public void undo(DebugState state) {
            parent.loadB(address, oldValue);
        }

        @Override
        public String getTrace() {
            return "%04X:%02X->%02X".formatted(address.value(), oldValue.value(), newValue.value());
        }
    }

    @Override
    public void writeB(Word address, UByte value, DebugState state) throws Fault {
        var action = new PlainMemoryWriteAction(this, address, value, readB(address, state));
        writeB(address, value);
        state.record(action);
    }

    @Override
    public byte[] dumpBytes() {
        var result = new byte[mem.length];
        for (int i = 0; i < mem.length; i++) {
            result[i] = (byte) mem[i].value();
        }
        return result;
    }
}
