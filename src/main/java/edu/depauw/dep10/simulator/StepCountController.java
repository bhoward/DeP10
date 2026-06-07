package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.ui.MainFrame;
import edu.depauw.dep10.util.Word;

public class StepCountController implements Controller {
    private Controller parent;
    private int max;
    private int step;

    public StepCountController(Controller parent, int max) {
        this.parent = parent;
        this.max = max;
        this.step = 0;
    }

    @Override
    public void perform(Operation op, State s, Word origPC) {
        parent.perform(op, s, origPC);
        
        step++;
        if (step >= max) {
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
    public void resume(MainFrame frame) {
        parent.resume(frame);
    }

    @Override
    public void forward(MainFrame frame) {
        parent.forward(frame);
    }
}
