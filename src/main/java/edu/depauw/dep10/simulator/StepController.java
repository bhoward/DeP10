package edu.depauw.dep10.simulator;

import edu.depauw.dep10.ModeOperation;
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
    public void perform(ModeOperation op, State s, Word pc) {
        parent.perform(op, s, pc);
        
        step++;
        if (step >= max) {
            s.stop();
        }
    }
}
