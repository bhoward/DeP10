package edu.depauw.dep10.simulator2;

/**
 * Represents a sequence of {@link Action}s that are treated as a single step
 * during debug mode execution.
 */
public interface Step {
    /**
     * Undo the effect of the sequence of actions.
     * 
     * @param state
     */
    void undo(State state);
}
