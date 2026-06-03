package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.Controller;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

public sealed interface TableEntry permits Operation, Operation.Prefix {
    void perform(State s, Word pc, Controller control);
}
