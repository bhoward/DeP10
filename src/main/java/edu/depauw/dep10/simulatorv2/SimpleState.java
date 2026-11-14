package edu.depauw.dep10.simulatorv2;

import java.util.ArrayList;
import java.util.List;

import edu.depauw.dep10.simulatorv2.RegisterFile.BReg;
import edu.depauw.dep10.simulatorv2.RegisterFile.FReg;
import edu.depauw.dep10.simulatorv2.RegisterFile.WReg;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * An implementation of {@link State} with a flat 64K
 * address space, with no protection.
 */
public class SimpleState implements State {
    public class SStep implements Step {
        private interface Action {
            void perform(SimpleState s);
            void undo(SimpleState s);
        }
        
        private record Write(Word address, UByte b, UByte old) implements Action {
            @Override
            public void perform(SimpleState s) {
                s.mem[address.value()] = b;
            }

            @Override
            public void undo(SimpleState s) {
                s.mem[address.value()] = old;
            }
        }
        
        private record SetW(WReg reg, Word w, Word old) implements Action {
            @Override
            public void perform(SimpleState s) {
                s.rf.set(reg, w);
            }

            @Override
            public void undo(SimpleState s) {
                s.rf.set(reg, old);
            }
        }
        
        private record SetB(BReg reg, UByte b, UByte old) implements Action {
            @Override
            public void perform(SimpleState s) {
                s.rf.set(reg, b);
            }

            @Override
            public void undo(SimpleState s) {
                s.rf.set(reg, old);
            }
        }
        
        private record SetFlag(FReg flag, boolean b, boolean old) implements Action {
            @Override
            public void perform(SimpleState s) {
                s.rf.set(flag, b);
            }

            @Override
            public void undo(SimpleState s) {
                s.rf.set(flag, old);
            }
        }
        
        private List<Action> actions = new ArrayList<>();
        
        public void writeB(Word address, UByte b) {
            actions.add(new Write(address, b, mem[address.value()]));
        }

        public void setWReg(WReg reg, Word w) {
            actions.add(new SetW(reg, w, rf.get(reg)));
        }

        public void setBReg(BReg reg, UByte b) {
            actions.add(new SetB(reg, b, rf.get(reg)));
        }

        public void setFlag(FReg flag, boolean b) {
            actions.add(new SetFlag(flag, b, rf.get(flag)));
        }
        
        @Override
        public void commit() {
            for (var action : actions) {
                action.perform(SimpleState.this);
            }
        }
        
        @Override
        public void rollback() {
            // Nothing to do for uncommitted actions
        }
        
        @Override
        public void undo() {
            for (var action : actions.reversed()) {
                action.undo(SimpleState.this);
            }
        }
    }
    
    private UByte[] mem = new UByte[65536];
    
    private RegisterFile rf;
    
    private SStep current;

    public SimpleState(RegisterFile rf) {
        this.rf = rf;
        this.current = new SStep();
    }
    
    @Override
    public UByte readB(Word address) {
        return mem[address.value()];
    }

    @Override
    public UByte fetchB(Word address) {
        return mem[address.value()];
    }

    @Override
    public void writeB(Word address, UByte b) {
        current.writeB(address, b);
    }

    @Override
    public Word getWReg(WReg reg) {
        return rf.get(reg);
    }

    @Override
    public UByte getBReg(BReg reg) {
        return rf.get(reg);
    }

    @Override
    public boolean getFlag(FReg flag) {
        return rf.get(flag);
    }

    @Override
    public void setWReg(WReg reg, Word w) {
        current.setWReg(reg, w);
    }

    @Override
    public void setBReg(BReg reg, UByte b) {
        current.setBReg(reg, b);
    }

    @Override
    public void setFlag(FReg flag, boolean b) {
        current.setFlag(flag, b);
    }

    @Override
    public Step commit() {
        var result = current;
        result.commit();
        current = new SStep();
        return result;
    }

    @Override
    public void rollback() {
        current.rollback();
        current = new SStep();
    }

    @Override
    public void undo(Step step) {
        if (step instanceof SStep s) {
            s.undo();
        }
        current = new SStep();
    }
}
