package edu.depauw.dep10.simulator2.pep10;

import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.common.BaseState;
import edu.depauw.dep10.simulator2.common.RegisterFile;

/**
 * Convenience subclass of BaseState with the Pep10Registers methods mixed in.
 */
public class Pep10State extends BaseState implements Pep10Registers {
    public Pep10State(Device memory, RegisterFile registerFile) {
        super(memory, registerFile);
    }
}