package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.ui.MainFrame;
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
            System.out.println("Step limit reached");
            pause();
        }
    }

    @Override
    public void end() {
        parent.end();
    }

    @Override
    public void pause() {
        parent.pause();
    }

    @Override
    public boolean isPaused() {
        return parent.isPaused();
    }

    @Override
    public Controller resume(MainFrame frame) {
        return parent.resume(frame);
    }
}
