package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public abstract class Mode {
    private int offset;
    private String suffix;
    
    private Mode(int offset, String suffix) {
        this.offset = offset;
        this.suffix = suffix;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public String getSuffix() {
        return suffix;
    }
        
    public abstract Word getAddress(State s);
    
    public Word resolveWord(State s) {
        var address = getAddress(s);
        s.setEA(address);
        return s.mem2(address);
    }
    
    public UByte resolveByte(State s) {
        var address = getAddress(s);
        s.setEA(address);
        return s.mem1(address);
    }
    
    public static final Mode I = new Mode(0, "i") {
        public Word getAddress(State s) {
            throw new IllegalStateException("Illegal addressing mode");
        } 
        
        @Override
        public Word resolveWord(State s) {
            return s.getOperand();
        }

        @Override
        public UByte resolveByte(State s) {
            return s.getOperand().lo();
        }
    };
    
    public static final Mode D = new Mode(1, "d") {
        public Word getAddress(State s) {
            return s.getOperand();
        } 
    };
    
    public static final Mode N = new Mode(2, "n") {
        public Word getAddress(State s) {
            return s.mem2(s.getOperand());
        }
    };
    
    public static final Mode S = new Mode(3, "s") {
        public Word getAddress(State s) {
            return s.getSP().plus(s.getOperand());
        }
    };
    
    public static final Mode SF = new Mode(4, "sf") {
        public Word getAddress(State s) {
            return s.mem2(s.getSP().plus(s.getOperand()));
        }
    };
    
    public static final Mode X = new Mode(5, "x") {
        public Word getAddress(State s) {
            return s.getOperand().plus(s.getX());
        }
    };
    
    // Variant of X with a different offset code
    public static final Mode X1 = new Mode(1, "x") {
        public Word getAddress(State s) {
            return X.getAddress(s);
        } 
    };
    
    public static final Mode SX = new Mode(6, "sx") {
        public Word getAddress(State s) {
            return s.getSP().plus(s.getOperand()).plus(s.getX());
        }
    };
    
    public static final Mode SFX = new Mode(7, "sfx") {
        public Word getAddress(State s) {
            return s.mem2(s.getSP().plus(s.getOperand())).plus(s.getX());
        }
    };
}
