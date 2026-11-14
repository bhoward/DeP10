package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Action;
import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class DebugPlainMemoryDevice extends PlainMemoryDevice {
    private record PlainMemoryWriteAction(Word address, UByte newValue, UByte oldValue) implements Action {
        
    }

    public DebugPlainMemoryDevice(int size) {
		super(size);
	}
    
    @Override
    public void writeB(Word address, UByte value, State state) throws Fault {
        var action = new PlainMemoryWriteAction(address, value, readB(address, state));
        super.writeB(address, value, state);
        ((DebugState) state).record(action);
    }
}
