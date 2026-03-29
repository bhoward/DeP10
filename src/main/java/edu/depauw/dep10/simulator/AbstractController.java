package edu.depauw.dep10.simulator;

import edu.depauw.dep10.ModeOperation;

public class AbstractController implements Controller {
    public void perform(ModeOperation op, State s) {
        op.apply(s);
    }
}
