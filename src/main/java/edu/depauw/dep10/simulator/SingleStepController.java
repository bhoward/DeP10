package edu.depauw.dep10.simulator;


import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.util.Word;

public class SingleStepController implements Controller {
    private Controller parent;

    public SingleStepController(Controller parent) {
        this.parent = parent;
    }

    @Override
    public boolean perform(Operation op, State s, Word origPC) {
        var result = parent.perform(op, s, origPC);
        pause();
        return result;
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
    public boolean isEnded() {
        return parent.isEnded();
    }

    @Override
    public boolean isPaused() {
        return parent.isPaused();
    }

    @Override
    public State saved() {
        return parent.saved();
    }
}
