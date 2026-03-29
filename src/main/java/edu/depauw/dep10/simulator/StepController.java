package edu.depauw.dep10.simulator;

import edu.depauw.dep10.ModeOperation;

public class StepController extends AbstractController {
    private int max;
    private int step;

    public StepController(int max) {
        this.max = max;
        this.step = 0;
    }

    @Override
    public void perform(ModeOperation op, State s) {
        super.perform(op, s);
        
        step++;
        if (step >= max) {
            s.setRunning(false);
        }
    }
}
