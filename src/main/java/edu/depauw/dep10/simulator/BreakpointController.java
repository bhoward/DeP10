package edu.depauw.dep10.simulator;

import java.util.function.Predicate;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.ui.MainFrame;
import edu.depauw.dep10.util.Word;

public class BreakpointController implements Controller {
    private Controller parent;
    private Predicate<State> pred;

    public BreakpointController(Controller parent, Predicate<State> pred) {
        this.parent = parent;
        this.pred = pred;
    }

    @Override
    public void perform(Operation op, State s, Word origPC) {
        parent.perform(op, s, origPC);
        if (pred.test(s)) {
            s.pause();
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
