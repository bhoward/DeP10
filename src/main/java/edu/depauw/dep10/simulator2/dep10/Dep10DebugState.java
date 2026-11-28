package edu.depauw.dep10.simulator2.dep10;

import edu.depauw.dep10.simulator2.MemoryDevice;
import edu.depauw.dep10.simulator2.common.DebugBaseState;
import edu.depauw.dep10.simulator2.common.RegisterFile;

/**
 * Convenience subclass of DebugBaseState with the Dep10Registers methods mixed
 * in.
 */
public class Dep10DebugState extends DebugBaseState implements Dep10Registers {
    public Dep10DebugState(MemoryDevice memory, RegisterFile registerFile) {
        super(memory, registerFile);
    }
}
