package edu.depauw.dep10.simulator;

import edu.depauw.dep10.ModeOperation;

public class PlainController implements Controller {
    public void perform(ModeOperation op, State s) {
        op.apply(s);
    }
}
