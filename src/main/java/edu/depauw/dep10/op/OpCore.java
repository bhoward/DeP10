package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;

public abstract class OpCore {
    private String mnemonic;
    private Mode[] modes;
    
    public OpCore(String mnemonic, Mode[] modes) {
        this.mnemonic = mnemonic;
        this.modes = modes;
    }

    public abstract void exec(State state, Mode mode);

    public String getMnemonic() {
        return mnemonic;
    }
    
    public Mode[] getModes() {
        return modes;
    }
}
