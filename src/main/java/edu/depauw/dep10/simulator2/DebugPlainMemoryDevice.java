package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class DebugPlainMemoryDevice extends PlainMemoryDevice {
    private record PlainMemoryWriteAction(Word address, UByte newValue, UByte oldValue) implements Action {
        
    }

    @Override
    public void writeB(Word address, UByte value, State state) throws Fault {
        var action = new PlainMemoryWriteAction(address, value, readB(address, state));
        super.writeB(address, value, state);
        ((DebugState) state).record(action);
    }
}
