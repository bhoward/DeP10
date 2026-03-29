package edu.depauw.dep10.simulator;

import edu.depauw.dep10.ModeOperation;
import edu.depauw.dep10.util.Word;

public class PlainController implements Controller {
    @Override
    public void perform(ModeOperation op, State s, Word pc) {
        op.apply(s);
    }
}
