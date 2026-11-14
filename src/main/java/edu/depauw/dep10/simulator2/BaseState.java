package edu.depauw.dep10.simulator2;

import java.util.ArrayList;
import java.util.List;

import edu.depauw.dep10.simulator2.common.RegisterFile;
import edu.depauw.dep10.simulator2.common.RegisterFile.ByteRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.FlagRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.WordRegister;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class BaseState extends AbstractState implements State {
    private Device memory;
    private List<Device> devices;
    private RegisterFile registerFile;

    public BaseState(Device memory, RegisterFile registerFile) {
        this.memory = memory;
        this.devices = new ArrayList<Device>();
        this.registerFile = registerFile;
    }
    
    public void mount(Device device) {
        devices.add(device);
    }
    
    // TODO is it worth speeding up the search through devices?
    // E.g., by only looking for devices mounted in 16-byte blocks of 0xFF00?
    
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
    public UByte get(ByteRegister register) {
        return registerFile.get(register);
    }
    
    @Override
    public void put(ByteRegister register, UByte value) {
        registerFile.put(register, value, this);
    }

    @Override
    public Word get(WordRegister register) {
        return registerFile.get(register);
    }

    @Override
    public void put(WordRegister register, Word value) {
        registerFile.put(register, value, this);
    }

    @Override
    public boolean get(FlagRegister flag) {
        return registerFile.get(flag);
    }
    
    @Override
    public void put(FlagRegister flag, boolean value) {
        registerFile.put(flag, value, this);
    }
}
