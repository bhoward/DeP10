package edu.depauw.dep10.simulator2;

/**
 * Records one modification performed as part of a {@link Step} for tracing
 * purposes, and so that it can be undone when stepping backwards.
 */
public interface Action {
    /**
     * Reverse the effect of this action.
     * 
     * @param state
     */
    void undo(DebugState state);

    /**
     * @return a string describing this action
     */
    String getTrace();
}
