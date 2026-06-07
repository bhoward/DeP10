package edu.depauw.dep10.simulator;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.ui.MainFrame;
import edu.depauw.dep10.util.Word;

public class TracingController implements Controller {
    public static int MAX_TRACE_LENGTH = 1000;
    
    private Controller parent;
    private Collection<Step> steps;
    
    public TracingController(Controller parent) {
        this.parent = parent;
        this.steps = new ArrayDeque<>(MAX_TRACE_LENGTH) {
            @Override
            public boolean add(Step e) {
                // Throw away old elements if full
                if (size() >= MAX_TRACE_LENGTH) {
                    super.remove();
                }
                return super.add(e);
            }
        };
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

    @Override
    public void end() {
        parent.end();
    }

    @Override
    public void pause() {
        parent.pause();
    }

    @Override
    public boolean isPaused() {
        return parent.isPaused();
    }

    @Override
    public void resume(MainFrame frame) {
        parent.resume(frame);
    }

    @Override
    public void forward(MainFrame frame) {
        parent.forward(frame);
    }
}
