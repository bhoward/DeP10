package edu.depauw.dep10.simulator2;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sequence of {@link Action}s that are treated as a single step
 * during debug mode execution.
 */
public class Step {
    private Operation operation;
    private Checkpoint checkpoint;
    private List<Action> actions;
    
    public Step(DebugState state) {
        this.operation = null;
        this.checkpoint = state.getCheckpoint();
        this.actions = new ArrayList<>();
    }
    
    /**
     * Undo the effect of the sequence of actions.
     * 
     * @param state
     */
    void undo(DebugState state) {
        for (var action : actions.reversed()) {
            action.undo(state);
        }
    }

    /**
     * Add the action to this step.
     * 
     * @param action
     */
    public void record(Action action) {
        actions.add(action);
    }

    public String getTrace() {
        var builder = new StringBuilder();
        
        builder.append(operation);
        builder.append(checkpoint.getTrace());
        for (var action : actions) {
            builder.append("\n  ");
            builder.append(action.getTrace());
        }
        
        return builder.toString();
    }

    public void setOperation(Operation op) {
        this.operation = op;
    }
}
