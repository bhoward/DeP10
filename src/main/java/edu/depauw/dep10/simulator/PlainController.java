package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.util.Word;

public class PlainController implements Controller {
    boolean endRequested = false;

    @Override
    public void perform(Operation op, State s, Word origPC) {
        if (endRequested) {
            s.stop();
        } else {
            op.exec(s);
        }
    }

    @Override
    public void end() {
        endRequested = true;
    }
}
