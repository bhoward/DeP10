package edu.depauw.dep10.simulator;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.util.Word;

public class TracingController implements Controller {
    private Controller parent;
    private List<Step> steps;
    
    public TracingController(Controller parent) {
        this.parent = parent;
        this.steps = new ArrayList<>();
    }

    @Override
    public void perform(Operation op, State s, Word origPC) {
        if (s instanceof DebugState ds) {
            ds.clearAccesses();
            parent.perform(op, s, origPC);
            steps.add(new Step(origPC, s.getPrefix(), s.getOpCode(), op, s.getOperand(), s.getEA(), ds.trace()));
        }
    }
    
    public void printTrace(PrintStream output) {
        for (var step : steps) {
            output.println(step);
        }
    }
}
