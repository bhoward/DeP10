package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.ui.MainFrame;
import edu.depauw.dep10.util.Word;

public class PlainController implements Controller {
    boolean ended = false;
    boolean paused = false;
    State saved = null;

    @Override
    public synchronized void perform(Operation op, State s, Word origPC) {
        if (ended) {
            s.stop();
        } else if (paused) {
            saved = s;
            s.pause();
        } else {
            op.exec(s);
        }
    }

    @Override
    public synchronized void end() {
        ended = true;
    }

    @Override
    public synchronized void pause() {
        paused = true;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public Controller resume(MainFrame frame) {
        return frame.getSourceType().resume(frame, saved);
    }
}
