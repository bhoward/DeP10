package edu.depauw.dep10.simulator2;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sequence of {@link Action}s that are treated as a single step
 * during debug mode execution.
 */
public class Step {
    private List<Action> actions;
    
    public Step() {
        this.actions = new ArrayList<>();
    }
    
    /**
     * Undo the effect of the sequence of actions.
     * 
     * @param state
     */
    void undo(State state) {
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
}
