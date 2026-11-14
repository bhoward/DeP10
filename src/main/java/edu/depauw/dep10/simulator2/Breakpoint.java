package edu.depauw.dep10.simulator2;

/**
 * Represents a condition that will be checked against each state during
 * execution in debug mode to see whether the simulation should be paused.
 */
public interface Breakpoint {
    /**
     * @param state
     * @return true if the state meets the breakpoint's condition to pause
     */
    boolean apply(State state);
}
