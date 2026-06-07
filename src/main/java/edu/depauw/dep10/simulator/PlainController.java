package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.ui.MainFrame;
import edu.depauw.dep10.util.Word;

public class PlainController implements Controller {
    boolean ended = false;
    boolean paused = false;
    State saved = null;

    @Override
    public synchronized boolean perform(Operation op, State s, Word origPC) {
        if (ended) {
            s.stop();
            return false;
        } else if (paused) {
            s.setPC(origPC);
            saved = s;
            s.pause();
            return false;
        } else {
            op.exec(s);
            if (!s.isRunning()) {
                saved = s; // in case of shutdown
            }
            return true;
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
    public boolean isEnded() {
        return ended;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void resume(MainFrame frame) {
        frame.getSourceType().resume(frame, saved);
    }
    
    @Override
    public void forward(MainFrame frame) {
        frame.getSourceType().forward(frame, saved);
    }
    
    @Override
    public void backward(MainFrame frame) {
        frame.getSourceType().backward(frame, saved);
    }
}
