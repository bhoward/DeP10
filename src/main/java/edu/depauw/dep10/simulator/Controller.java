package edu.depauw.dep10.simulator;

import edu.depauw.dep10.ModeOperation;
import edu.depauw.dep10.util.Word;

public interface Controller {
    void perform(ModeOperation op, State s, Word pc);
}
