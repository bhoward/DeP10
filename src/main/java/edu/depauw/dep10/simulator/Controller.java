package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.util.Word;

public interface Controller {
    void perform(Operation op, State s, Word pc);
}
