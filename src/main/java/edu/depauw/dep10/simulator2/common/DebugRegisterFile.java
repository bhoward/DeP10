package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Action;
import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * A version of the {@link RegisterFile} that will record actions for playback
 * in debug mode.
 */
public class DebugRegisterFile extends RegisterFile {
    private record ByteRegisterPutAction(RegisterFile parent, ByteRegister register, UByte newValue, UByte oldValue)
            implements Action {
        @Override
        public void undo(State state) {
            parent.put(register, oldValue);
        }
    }

    private record WordRegisterPutAction(RegisterFile parent, WordRegister register, Word newValue, Word oldValue)
            implements Action {
        @Override
        public void undo(State state) {
            parent.put(register, oldValue);
        }
    }

    private record FlagRegisterPutAction(RegisterFile parent, FlagRegister flag, boolean newValue, boolean oldValue)
            implements Action {
        @Override
        public void undo(State state) {
            parent.put(flag, oldValue);
        }
    }

    /**
     * Construct a DebugRegisterFile with the given number of byte and word
     * registers. Any flags should be part of one of the byte registers, so they do
     * not need to be accounted for separately.
     * 
     * @param numByteRegisters
     * @param numWordRegisters
     */
    public DebugRegisterFile(int numByteRegisters, int numWordRegisters) {
        super(numByteRegisters, numWordRegisters);
    }

    /**
     * Put a byte into the given register, and record any action that might need to
     * be undone with the given state.
     * 
     * @param register
     * @param value
     * @param state
     */
    public void put(ByteRegister register, UByte value, State state) {
        var action = new ByteRegisterPutAction(this, register, value, get(register));
        put(register, value);
        ((DebugState) state).record(action);
    }

    /**
     * Put a byte into the given register, and record any action that might need to
     * be undone with the given state.
     * 
     * @param register
     * @param value
     * @param state
     */
    public void put(WordRegister register, Word value, State state) {
        var action = new WordRegisterPutAction(this, register, value, get(register));
        put(register, value);
        ((DebugState) state).record(action);
    }

    /**
     * Put a flag bit into the given register, and record any action that might need
     * to be undone with the given state.
     * 
     * @param register
     * @param value
     * @param state
     */
    public void put(FlagRegister flag, boolean value, State state) {
        var action = new FlagRegisterPutAction(this, flag, value, get(flag));
        put(flag, value);
        ((DebugState) state).record(action);
    }

    /**
     * Reverse the last tick(), if possible (in debug mode).
     */
    public void untick() {
        // Do nothing
    }
}
