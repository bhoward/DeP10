package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Action;
import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class DebugRegisterFile extends RegisterFile {
    private record ByteRegisterPutAction(ByteRegister register, UByte newValue, UByte oldValue) implements Action {

    }

    private record WordRegisterPutAction(WordRegister register, Word newValue, Word oldValue) implements Action {

    }

    private record FlagRegisterPutAction(FlagRegister register, boolean newValue, boolean oldValue) implements Action {

    }

    public DebugRegisterFile(int numByteRegisters, int numWordRegisters) {
        super(numByteRegisters, numWordRegisters);
    }

    @Override
    public void put(ByteRegister register, UByte value, State state) {
        var action = new ByteRegisterPutAction(register, value, get(register));
        super.put(register, value, state);
        ((DebugState) state).record(action);
    }

    @Override
    public void put(WordRegister register, Word value, State state) {
        var action = new WordRegisterPutAction(register, value, get(register));
        super.put(register, value, state);
        ((DebugState) state).record(action);
    }

    @Override
    public void put(FlagRegister flag, boolean value, State state) {
        var action = new FlagRegisterPutAction(flag, value, get(flag));
        super.put(flag, value, state);
        ((DebugState) state).record(action);
    }
}
