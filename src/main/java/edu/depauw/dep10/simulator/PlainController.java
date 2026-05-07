package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.util.Word;

public class PlainController implements Controller {
    @Override
    public void perform(Operation op, State s, Word pc) {
        op.exec(s);
    }
}
