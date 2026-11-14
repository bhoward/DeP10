package edu.depauw.dep10.simulator2;

/**
 * Represents a particular configuration of hardware: instruction set, operation
 * cycle, registers, and memory-mapped devices.
 */
public interface Machine {
    State init();

    DebugState initDebug();

    Operation fetch(State state) throws Fault;

    void handle(Fault fault, State state);
}
