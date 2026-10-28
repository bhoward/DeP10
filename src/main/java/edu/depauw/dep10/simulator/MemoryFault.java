package edu.depauw.dep10.simulator;

import edu.depauw.dep10.util.Word;

/**
 * Thrown by the memory protection check when a user-mode instruction touches a page its
 * protection bits do not allow. State.doStep catches it, undoes the instruction and takes the
 * protection trap.
 */
public class MemoryFault extends RuntimeException {
    /** Trap operand for a read or write the table does not allow. */
    public static final int DATA = 3;
    /** Trap operand for an instruction fetch from a page that is not executable. */
    public static final int FETCH = 4;

    private final Word address;
    private final int operand;

    public MemoryFault(Word address, int operand) {
        super(String.format("Memory protection fault at 0x%04X (%s)", address.value(), operand == FETCH ? "fetch" : "data"));
        this.address = address;
        this.operand = operand;
    }

    public Word address() {
        return address;
    }

    /** The operand of the trap: DATA or FETCH. */
    public int operand() {
        return operand;
    }
}
