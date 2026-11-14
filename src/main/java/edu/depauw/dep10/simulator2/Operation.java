package edu.depauw.dep10.simulator2;

/**
 * Represents a single machine instruction.
 */
public interface Operation {
    void execute(State state);
}
