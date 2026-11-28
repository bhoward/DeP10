package edu.depauw.dep10.simulator2.common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.depauw.dep10.simulator2.AbstractState;
import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.MemoryDevice;
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
    protected MemoryDevice memory;
    protected List<Device> devices; // all mounted devices, to be ticked
    private final List<List<Device>> slices; // devices mounted in each 16-byte slice of the I/O page
    private static final List<Device> NO_DEVICES = List.of();
    protected RegisterFile registerFile;

    /**
     * Construct a {@link State} with the given memory and registers.
     * 
     * @param memory
     * @param registerFile
     */
    public BaseState(MemoryDevice memory, RegisterFile registerFile) {
        this.memory = memory;
        this.devices = new ArrayList<Device>();
        this.slices = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            slices.add(new ArrayList<>());
        }
        this.registerFile = registerFile;
    }

    /**
     * Add a {@link Device} to this state. Devices live in the I/O page, 0xFF00 to
     * 0xFFFF, which is divided into sixteen slices of 16 bytes each. The device is
     * mounted in the slice that contains the given address, and it is only asked
     * about addresses in that slice; every other address goes straight to memory.
     * Several devices may share a slice. The address does not have to be the first
     * one in the slice.
     * 
     * @param address an address in the slice where the device is mounted
     * @param device
     * @throws IllegalArgumentException if the address is not in the I/O page
     */
    public void mount(Word address, Device device) {
        if (!inIoPage(address)) {
            throw new IllegalArgumentException("Devices can only be mounted in the I/O page (0xFF00 to 0xFFFF): " + address);
        }

        slices.get(sliceOf(address)).add(device);
        devices.add(device);
    }

    private static boolean inIoPage(Word address) {
        return (address.value() >> 8) == 0xFF;
    }

    private static int sliceOf(Word address) {
        return (address.value() >> 4) & 0xF;
    }

    /**
     * @param address
     * @return the devices that might handle the given address: those mounted in its
     *         slice of the I/O page, or none for any other address
     */
    protected List<Device> devicesAt(Word address) {
        if (inIoPage(address)) {
            return slices.get(sliceOf(address));
        } else {
            return NO_DEVICES;
        }
    }

    @Override
    public UByte fetchB(Word address) throws Fault {
        for (var device : devicesAt(address)) {
            if (device.canFetchB(address)) {
                return device.fetchB(address);
            }
        }

        return memory.fetchB(address);
    }

    @Override
    public UByte readB(Word address) throws Fault {
        for (var device : devicesAt(address)) {
            if (device.canReadB(address)) {
                return device.readB(address);
            }
        }

        return memory.readB(address);
    }

    @Override
    public void writeB(Word address, UByte value) throws Fault {
        for (var device : devicesAt(address)) {
            if (device.canWriteB(address)) {
                device.writeB(address, value);
            }
        }

        memory.writeB(address, value);
    }

    @Override
    public Word fetchW(Word address) throws Fault {
        for (var device : devicesAt(address)) {
            if (device.canFetchW(address)) {
                return device.fetchW(address);
            }
        }

        return memory.fetchW(address);
    }

    @Override
    public Word readW(Word address) throws Fault {
        for (var device : devicesAt(address)) {
            if (device.canReadW(address)) {
                return device.readW(address);
            }
        }

        return memory.readW(address);
    }

    @Override
    public void writeW(Word address, Word value) throws Fault {
        for (var device : devicesAt(address)) {
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

    @Override
    public void dump(String fileName) {
        try (var dumpOut = new FileOutputStream(fileName)) {
            var mem = memory.dumpBytes();
            dumpOut.write(mem);
        } catch (IOException e) {
            // TODO log this?
            System.err.println("Error writing memory dump: " + e.getMessage());
        }
    }
}
