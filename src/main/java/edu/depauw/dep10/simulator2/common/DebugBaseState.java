package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Action;
import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.Step;
import edu.depauw.dep10.simulator2.common.RegisterFile.ByteRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.FlagRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.WordRegister;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * A version of {@link BaseState} for use in debug mode.
 */
public class DebugBaseState extends BaseState implements DebugState {
    private Step current;

    public DebugBaseState(Device memory, DebugRegisterFile registerFile) {
        super(memory, registerFile);
        this.current = new Step();
    }

    @Override
    public Step getStep() {
        var result = current;
        current = new Step();
        return result;
    }

    @Override
    public void record(Action action) {
        current.record(action);
    }

    // Use the debug versions of putting values into registers

    @Override
    public void put(ByteRegister register, UByte value) {
        ((DebugRegisterFile) registerFile).put(register, value, this);
    }

    @Override
    public void put(WordRegister register, Word value) {
        ((DebugRegisterFile) registerFile).put(register, value, this);
    }

    @Override
    public void put(FlagRegister flag, boolean value) {
        ((DebugRegisterFile) registerFile).put(flag, value, this);
    }

    @Override
    public void untick() {
        for (var device : devices) {
            device.untick();
        }
        memory.untick();
        ((DebugRegisterFile) registerFile).untick();
    }
}
