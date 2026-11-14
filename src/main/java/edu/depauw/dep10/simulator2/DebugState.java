package edu.depauw.dep10.simulator2;

/**
 * Represents a {@link State} that will also record the {@link Action}s taken by
 * each {@link Step}, to allow them to be replayed in debug mode.
 */
public interface DebugState extends State {
    /**
     * @return an object collecting all of the actions taken since the last call
     */
    Step getStep();

    /**
     * Add an action to the current step.
     * 
     * @param action
     */
    void record(Action action);
}
