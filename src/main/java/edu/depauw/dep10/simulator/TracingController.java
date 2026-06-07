package edu.depauw.dep10.simulator;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.ui.MainFrame;
import edu.depauw.dep10.util.Word;

public class TracingController implements Controller {
    public static int MAX_TRACE_LENGTH = 1000;

    private Controller parent;
    private Deque<Step> steps;

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

    public TracingController(Controller parent, Controller previous) {
        this(parent);
        if (previous instanceof TracingController tc) {
            // Preserve old trace
            this.steps = tc.steps;
        }
    }

    @Override
    public boolean perform(Operation op, State s, Word origPC) {
        if (s instanceof DebugState ds) {
            ds.clearAccesses();
            if (parent.perform(op, s, origPC)) {
                // Only save completed steps
                steps.add(new Step(origPC, s.getPrefix(), s.getOpCode(), op, s.getOperand(), s.getEA(), ds.trace()));
                return true;
            } else {
                return false;
            }
        } else {
            // shouldn't happen
            return false;
        }
    }

    public void printTrace(PrintStream output) {
        for (var step : steps) {
            output.println(step);
        }
    }
    
    public void goBackward(State state) {
        if (steps.size() >= 2) {
            var last = steps.removeLast();
            var prev = steps.getLast();
            state.setA(prev.trace().a());
            state.setX(prev.trace().x());
            state.setFlags(prev.trace().flags());
            state.setSP(prev.trace().sp());
            state.setPC(last.pc());
            state.setOp(prev.op());
            state.setPrefix(prev.prefix());
            state.setOpCode(prev.opCode());
            state.setOperand(prev.operand());
            state.setEA(prev.address());
            for (var access : last.trace().accesses()) {
                if (access instanceof MemoryAccess.WB write) {
                    state.setMem1(write.addr(), write.prev()); // TODO handle charIn
                }
            }
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
    public boolean isEnded() {
        return parent.isEnded();
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

    @Override
    public void backward(MainFrame frame) {
        parent.backward(frame);
    }
}
