package edu.depauw.dep10.simulatorv2;

import edu.depauw.dep10.simulatorv2.MMIOState.MStep;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class ConsoleDevice implements IODevice {
    private Word charIn;
    private Word charOut;
    private Simulator simulator;

    public ConsoleDevice(Word location, Simulator simulator) {
        this.charIn = location;
        this.charOut = location.plus(1);
        this.simulator = simulator;
    }

    @Override
    public boolean canRead(Word address) {
        return address.equals(charIn);
    }

    @Override
    public boolean canFetch(Word address) {
        return false;
    }

    @Override
    public boolean canWrite(Word address) {
        return address.equals(charOut);
    }

    @Override
    public UByte readB(Word offset, MStep current) {
        int ch = simulator.read();
        current.add(new MStep.Action() {
            @Override
            public void perform(MMIOState s) {
                // do nothing
            }

            @Override
            public void undo(MMIOState s) {
                simulator.unread(ch);
            }
        });
        return UByte.of(ch);
    }

    @Override
    public void writeB(Word offset, UByte b, MStep current) {
        current.add(new MStep.Action() {
            @Override
            public void perform(MMIOState s) {
                simulator.write(b.value());
            }

            @Override
            public void undo(MMIOState s) {
                // do nothing -- too late
            }
        });
    }
}
