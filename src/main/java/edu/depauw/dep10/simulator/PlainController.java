package edu.depauw.dep10.simulator;


import edu.depauw.dep10.op.Operation;
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
    public State saved() {
        return saved;
    }
}
