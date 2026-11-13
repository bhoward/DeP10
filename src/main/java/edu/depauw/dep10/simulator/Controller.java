package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.util.Word;

public interface Controller {
    boolean perform(Operation op, State s, Word pc);
    
    void end();
    
    void pause();
    
    boolean isEnded();
    
    boolean isPaused();

    /**
     * The state at which this controller (or the controller it wraps) last paused or stopped, or null if
     * it never did. A user interface uses it to resume, single-step or step back from that point.
     */
    State saved();
}
