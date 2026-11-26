package edu.depauw.dep10.simulator2.dep10;

import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.common.BaseState;
import edu.depauw.dep10.simulator2.common.RegisterFile;

/**
 * Convenience subclass of BaseState with the Dep10Registers methods mixed in.
 */
public class Dep10State extends BaseState implements Dep10Registers {
    public Dep10State(Device memory, RegisterFile registerFile) {
        super(memory, registerFile);
    }
}
