package edu.depauw.dep10.simulator2;

/**
 * Represents a snapshot of the current state (e.g., the contents of registers)
 * to be recorded in a {@link Step} for tracing purposes.
 */
public interface Checkpoint {
    String getTrace();
}
