package edu.depauw.dep10.simulator2.pep10;

import edu.depauw.dep10.simulator2.DebugBaseState;
import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.common.RegisterFile;

public class Pep10DebugState extends DebugBaseState implements Pep10Registers {
    public Pep10DebugState(Device memory, RegisterFile registerFile) {
        super(memory, registerFile);
    }
}