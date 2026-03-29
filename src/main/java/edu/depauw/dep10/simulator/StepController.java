package edu.depauw.dep10.simulator;

import edu.depauw.dep10.ModeOperation;

public class StepController implements Controller {
    private Controller parent;
    private int max;
    private int step;

    public StepController(Controller parent, int max) {
        this.parent = parent;
        this.max = max;
        this.step = 0;
    }

    public void perform(ModeOperation op, State s) {
        parent.perform(op, s);
        
        step++;
        if (step >= max) {
            s.stop();
        }
    }
}
