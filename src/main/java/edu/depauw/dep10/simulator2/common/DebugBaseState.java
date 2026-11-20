package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Action;
import edu.depauw.dep10.simulator2.DebugDevice;
import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.simulator2.Fault;
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

    public DebugBaseState(DebugDevice memory, DebugRegisterFile registerFile) {
        super(memory, registerFile);
        this.current = new Step();
    }

    /**
     * Add a {@link DebugDevice} to this state.
     * 
     * @param device
     */
    public void mount(DebugDevice device) {
        super.mount(device);
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

    // Use the debug versions to access memory and registers

    @Override
    public UByte fetchB(Word address) throws Fault {
        for (var device : devices) {
            if (device.canFetchB(address)) {
                return ((DebugDevice) device).fetchB(address, this);
            }
        }

        return ((DebugDevice) memory).fetchB(address, this);
    }

    @Override
    public UByte readB(Word address) throws Fault {
        for (var device : devices) {
            if (device.canReadB(address)) {
                return ((DebugDevice) device).readB(address, this);
            }
        }

        return ((DebugDevice) memory).readB(address, this);
    }

    @Override
    public void writeB(Word address, UByte value) throws Fault {
        for (var device : devices) {
            if (device.canWriteB(address)) {
                ((DebugDevice) device).writeB(address, value, this);
            }
        }

        ((DebugDevice) memory).writeB(address, value, this);
    }

    @Override
    public Word fetchW(Word address) throws Fault {
        for (var device : devices) {
            if (device.canFetchW(address)) {
                return ((DebugDevice) device).fetchW(address, this);
            }
        }

        return ((DebugDevice) memory).fetchW(address, this);
    }

    @Override
    public Word readW(Word address) throws Fault {
        for (var device : devices) {
            if (device.canReadW(address)) {
                return ((DebugDevice) device).readW(address, this);
            }
        }

        return ((DebugDevice) memory).readW(address, this);
    }

    @Override
    public void writeW(Word address, Word value) throws Fault {
        for (var device : devices) {
            if (device.canWriteW(address)) {
                ((DebugDevice) device).writeW(address, value, this);
            }
        }

        // Note that writing a word across two adjacent devices will go to memory...
        ((DebugDevice) memory).writeW(address, value, this);
    }

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
            ((DebugDevice) device).untick();
        }
        ((DebugDevice) memory).untick();
        ((DebugRegisterFile) registerFile).untick();
    }
}
