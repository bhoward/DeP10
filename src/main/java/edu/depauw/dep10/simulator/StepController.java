package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.util.Word;

public class StepController implements Controller {
    private Controller parent;
    private int max;
    private int step;

    public StepController(Controller parent, int max) {
        this.parent = parent;
        this.max = max;
        this.step = 0;
    }

    @Override
    public void perform(Operation op, State s, Word origPC) {
        parent.perform(op, s, origPC);
        
        step++;
        if (step >= max) {
            s.stop();
        }
    }
}
