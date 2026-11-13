package edu.depauw.dep10.simulator2;

import java.util.ArrayList;
import java.util.List;

public class DebugController extends AbstractController {
    private List<Breakpoint> breakpoints;
    private List<Step> steps;

    public DebugController(Machine machine) {
        super(machine);
        this.breakpoints = new ArrayList<>();
    }

    public void clearBreakpoints() {
        breakpoints.clear();
    }

    public void addBreakPoint(Breakpoint breakpoint) {
        breakpoints.add(breakpoint);
    }

    @Override
    public void run() {
        pause(); // stop any previous run

        state = machine.initDebug();
        steps = new ArrayList<>();
        startCycle();
    }

    public void stepForward() {
        if (isPaused()) {
            var op = machine.fetch(state);
            op.execute(state);

            // Capture the state changes since the last call to getStep()
            var step = ((DebugState) state).getStep();
            steps.add(step);
        }
    }

    public void stepReverse() {
        if (isPaused() && !steps.isEmpty()) {
            var step = steps.removeLast();
            step.undo(state);
        }
    }

    @Override
    protected void doCycle() {
        while (state.isRunning()) {
            stepForward();

            // Check for breakpoints
            for (var breakpoint : breakpoints) {
                if (breakpoint.apply(state)) {
                    state.pause();
                }
            }
        }
    }
}
