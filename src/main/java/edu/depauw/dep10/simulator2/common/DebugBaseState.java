package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Action;
import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.simulator2.Device;
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

    public DebugBaseState(Device memory, RegisterFile registerFile) {
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

    // Use the debug versions to access memory and registers

    @Override
    public UByte fetchB(Word address) throws Fault {
        for (var device : devices) {
            if (device.canFetchB(address)) {
                return device.fetchB(address, this);
            }
        }

        return memory.fetchB(address, this);
    }

    @Override
    public UByte readB(Word address) throws Fault {
        for (var device : devices) {
            if (device.canReadB(address)) {
                return device.readB(address, this);
            }
        }

        return memory.readB(address, this);
    }

    @Override
    public void writeB(Word address, UByte value) throws Fault {
        for (var device : devices) {
            if (device.canWriteB(address)) {
                device.writeB(address, value, this);
            }
        }

        memory.writeB(address, value, this);
    }

    @Override
    public Word fetchW(Word address) throws Fault {
        for (var device : devices) {
            if (device.canFetchW(address)) {
                return device.fetchW(address, this);
            }
        }

        return memory.fetchW(address, this);
    }

    @Override
    public Word readW(Word address) throws Fault {
        for (var device : devices) {
            if (device.canReadW(address)) {
                return device.readW(address, this);
            }
        }

        return memory.readW(address, this);
    }

    @Override
    public void writeW(Word address, Word value) throws Fault {
        for (var device : devices) {
            if (device.canWriteW(address)) {
                device.writeW(address, value, this);
            }
        }

        // Note that writing a word across two adjacent devices will go to memory...
        memory.writeW(address, value, this);
    }

    @Override
    public void put(ByteRegister register, UByte value) {
        registerFile.put(register, value, this);
    }

    @Override
    public void put(WordRegister register, Word value) {
        registerFile.put(register, value, this);
    }

    @Override
    public void put(FlagRegister flag, boolean value) {
        registerFile.put(flag, value, this);
    }

    @Override
    public void untick() {
        for (var device : devices) {
            device.untick();
        }
        memory.untick();
        registerFile.untick();
    }
}
