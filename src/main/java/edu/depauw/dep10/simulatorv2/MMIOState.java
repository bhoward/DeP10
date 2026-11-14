package edu.depauw.dep10.simulatorv2;

import java.util.ArrayList;
import java.util.List;

import edu.depauw.dep10.simulatorv2.RegisterFile.BReg;
import edu.depauw.dep10.simulatorv2.RegisterFile.FReg;
import edu.depauw.dep10.simulatorv2.RegisterFile.WReg;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Decorate a {@link State} implementation with memory-mapped I/O devices.
 */
public class MMIOState implements State {
    public class MStep implements Step {
        public interface Action {
            void perform(MMIOState s);

            void undo(MMIOState s);
        }
        
        private List<Action> actions = new ArrayList<>();
        private Step parent = null;

        public void add(Action action) {
            actions.add(action);
        }
        
        @Override
        public void commit() {
            parent = base.commit();
            
            for (var action : actions) {
                action.perform(MMIOState.this);
            }
        }
        
        @Override
        public void rollback() {
            for (var action : actions.reversed()) {
                action.undo(MMIOState.this);
            }
            
            base.rollback();
        }
        
        @Override
        public void undo() {
            for (var action : actions.reversed()) {
                action.undo(MMIOState.this);
            }
            
            if (parent != null) {
                parent.undo();
            }
        }
    }
    
    private State base;
    private List<IODevice> devices;
    
    private MStep current;

    public MMIOState(State state) {
        this.base = state;
        this.devices = new ArrayList<>();
        
        this.current = new MStep();
    }
    
    public void install(IODevice device) {
        devices.add(device);
    }

    @Override
    public UByte readB(Word address) {
        for (var device : devices) {
            if (device.canRead(address)) {
                return device.readB(address, current);
            }
        }
        
        return base.readB(address);
    }

    @Override
    public UByte fetchB(Word address) {
        for (var device : devices) {
            if (device.canFetch(address)) {
                return device.fetchB(address, current);
            }
        }
        
        return base.fetchB(address);
    }

    @Override
    public void writeB(Word address, UByte b) {
        for (var device : devices) {
            if (device.canWrite(address)) {
                device.writeB(address, b, current);
            }
        }
        
        base.writeB(address, b);
    }

    @Override
    public Word getWReg(WReg reg) {
        return base.getWReg(reg);
    }

    @Override
    public UByte getBReg(BReg reg) {
        return base.getBReg(reg);
    }

    @Override
    public boolean getFlag(FReg flag) {
        return base.getFlag(flag);
    }

    @Override
    public void setWReg(WReg reg, Word w) {
        base.setWReg(reg, w);
    }

    @Override
    public void setBReg(BReg reg, UByte b) {
        base.setBReg(reg, b);
    }

    @Override
    public void setFlag(FReg flag, boolean b) {
        base.setFlag(flag, b);
    }

    @Override
    public Step commit() {
        var result = current;
        result.commit();
        current = new MStep();
        return result;
    }

    @Override
    public void rollback() {
        current.rollback();
        current = new MStep();
    }

    @Override
    public void undo(Step step) {
        if (step instanceof MStep s) {
            s.undo();
        }
        current = new MStep();
    }
}
