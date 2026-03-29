package edu.depauw.dep10.simulator;

import edu.depauw.dep10.ModeOperation;

public interface Controller {
    void perform(ModeOperation op, State s);
}
