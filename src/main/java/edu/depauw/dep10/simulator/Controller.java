package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.ui.MainFrame;
import edu.depauw.dep10.util.Word;

public interface Controller {
    boolean perform(Operation op, State s, Word pc);
    
    void end();
    
    void pause();
    
    boolean isPaused();

    void resume(MainFrame frame);

    void forward(MainFrame frame);
}
