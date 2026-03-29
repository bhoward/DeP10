package edu.depauw.dep10;

import edu.depauw.dep10.simulator.State;

public record ModeOperation(AddrMode mode, Operation op) {
    public boolean hasOperand() {
        return op.hasOperand();
    }

    public void apply(State s) {
        op.apply(s, mode);
    }

    @Override
    public String toString() {
        if (mode != null) {
            return String.format("%-7s", op + "," + mode);
        } else {
            return String.format("%-7s", op);
        }
    }
}
