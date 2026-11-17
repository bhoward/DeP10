package edu.depauw.dep10.simulator2;

/**
 * Represents a particular configuration of hardware: instruction set, operation
 * cycle, registers, and memory-mapped devices.
 */
public interface Machine {
    /**
     * Initialize a machine state for normal execution with the given image.
     * 
     * @param image
     * @return the state
     */
    State init(Image image);

    /**
     * Initialize a machine state for debug execution with the given image.
     * 
     * @param image
     * @return the state
     */
    DebugState initDebug(Image image);

    /**
     * Fetch the next instruction for execution.
     * 
     * @param state
     * @return the instruction
     * @throws Fault
     */
    Operation fetch(State state) throws Fault;

    /**
     * Handle a machine fault in the given state.
     * 
     * @param fault
     * @param state
     */
    void handle(Fault fault, State state);
}
