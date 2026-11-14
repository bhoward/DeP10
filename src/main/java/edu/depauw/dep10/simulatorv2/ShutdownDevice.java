package edu.depauw.dep10.simulatorv2;

import edu.depauw.dep10.simulatorv2.MMIOState.MStep;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class ShutdownDevice implements IODevice {
    private Word location;
    private Simulator simulator;

    public ShutdownDevice(Word location, Simulator simulator) {
        this.location = location;
        this.simulator = simulator;
    }

    @Override
    public boolean canRead(Word address) {
        return false;
    }

    @Override
    public boolean canFetch(Word address) {
        return false;
    }

    @Override
    public boolean canWrite(Word address) {
        return address.equals(location);
    }

    @Override
    public void writeB(Word offset, UByte b, MStep current) {
        current.add(new MStep.Action() {
            @Override
            public void perform(MMIOState s) {
                simulator.setRunning(false);
            }

            @Override
            public void undo(MMIOState s) {
                simulator.setRunning(true);
            }
        });
    }
}
