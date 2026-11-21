package edu.depauw.dep10.simulator2.common;

import java.util.ArrayList;
import java.util.List;

import edu.depauw.dep10.simulator2.AbstractState;
import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.Operation;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.simulator2.common.RegisterFile.ByteRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.FlagRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.WordRegister;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * An implementation of {@link State} with memory-mapped I/O devices and a
 * register file.
 */
public class BaseState extends AbstractState implements State {
    protected Device memory;
    protected List<Device> devices;
    protected RegisterFile registerFile;

    /**
     * Construct a {@link State} with the given memory and registers.
     * 
     * @param memory
     * @param registerFile
     */
    public BaseState(Device memory, RegisterFile registerFile) {
        this.memory = memory;
        this.devices = new ArrayList<Device>();
        this.registerFile = registerFile;
    }

    /**
     * Add a {@link Device} to this state.
     * 
     * @param device
     */
    public void mount(Device device) {
        devices.add(device);
    }

    // TODO is it worth speeding up the search through devices?
    // E.g., by only looking for devices mounted in 16-byte blocks of 0xFF00?

    @Override
    public UByte fetchB(Word address) throws Fault {
        for (var device : devices) {
            if (device.canFetchB(address)) {
                return device.fetchB(address);
            }
        }

        return memory.fetchB(address);
    }

    @Override
    public UByte readB(Word address) throws Fault {
        for (var device : devices) {
            if (device.canReadB(address)) {
                return device.readB(address);
            }
        }

        return memory.readB(address);
    }

    @Override
    public void writeB(Word address, UByte value) throws Fault {
        for (var device : devices) {
            if (device.canWriteB(address)) {
                device.writeB(address, value);
            }
        }

        memory.writeB(address, value);
    }

    @Override
    public Word fetchW(Word address) throws Fault {
        for (var device : devices) {
            if (device.canFetchW(address)) {
                return device.fetchW(address);
            }
        }

        return memory.fetchW(address);
    }

    @Override
    public Word readW(Word address) throws Fault {
        for (var device : devices) {
            if (device.canReadW(address)) {
                return device.readW(address);
            }
        }

        return memory.readW(address);
    }

    @Override
    public void writeW(Word address, Word value) throws Fault {
        for (var device : devices) {
            if (device.canWriteW(address)) {
                device.writeW(address, value);
            }
        }

        // Note that writing a word across two adjacent devices will go to memory...
        memory.writeW(address, value);
    }

    @Override
    public UByte get(ByteRegister register) {
        return registerFile.get(register);
    }

    @Override
    public void put(ByteRegister register, UByte value) {
        registerFile.put(register, value);
    }

    @Override
    public Word get(WordRegister register) {
        return registerFile.get(register);
    }

    @Override
    public void put(WordRegister register, Word value) {
        registerFile.put(register, value);
    }

    @Override
    public boolean get(FlagRegister flag) {
        return registerFile.get(flag);
    }

    @Override
    public void put(FlagRegister flag, boolean value) {
        registerFile.put(flag, value);
    }

    @Override
    public void tick() {
        registerFile.tick();
        memory.tick();
        for (var device : devices) {
            device.tick();
        }
    }

    @Override
    public void setOperation(Operation op) {
        // Ignore when not in debug mode
    }
}
