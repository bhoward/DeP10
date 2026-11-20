package edu.depauw.dep10.simulator2.pep10;

import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.common.DebugBaseState;
import edu.depauw.dep10.simulator2.common.RegisterFile;

/**
 * Convenience subclass of DebugBaseState with the Pep10Registers methods mixed
 * in.
 */
public class Pep10DebugState extends DebugBaseState implements Pep10Registers {
    public Pep10DebugState(Device memory, RegisterFile registerFile) {
        super(memory, registerFile);
    }
}