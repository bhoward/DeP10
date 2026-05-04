package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.Controller;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

public sealed interface Operation extends TableEntry {
    String getMnemonic();
    
    void exec(State state);

    boolean hasOperand();
    
    public static final Operation UNIMPLEMENTED = new Unary("UNIMP") {
        public void exec(State state) {
            throw new IllegalStateException("Unimplemented operation");
        }       
    };
    
    public abstract non-sealed class Unary implements Operation {
        private String mnemonic;
        
        public Unary(String mnemonic) {
            this.mnemonic = mnemonic;
        }
        
        public String getMnemonic() {
            return mnemonic;
        }
        
        public void perform(State s, Word pc, Controller control) {
            control.perform(this, s, pc);
        }
        
        @Override
        public String toString() {
            return String.format("%-7s", mnemonic);
        }
        
        public boolean hasOperand() {
            return false;
        }
    }

    public non-sealed class NonUnary implements Operation {
        private OpCore op;
        private Mode mode;
        
        public NonUnary(OpCore op, Mode mode) {
            this.op = op;
            this.mode = mode;
        }
        
        public String getMnemonic() {
            return op.getMnemonic();
        }

        public void exec(State s) {
            var pc = s.getPC();
            var operand = s.mem2(pc);
            s.setPC(pc.plus(2));
            s.setOperand(operand);
            op.exec(s, mode);
        }
        
        public void perform(State s, Word pc, Controller control) {
            control.perform(this, s, pc);
        }
        
       @Override
        public String toString() {
            return String.format("%-7s", op.getMnemonic() + "," + mode.getSuffix());
        }
        
        public boolean hasOperand() {
            return true;
        }
    }

    public final class Prefix implements TableEntry {
        private Table table;
        
        public Prefix(Table table) {
            this.table = table;
        }
        
        public void perform(State s, Word origpc, Controller control) {
            table.perform(s, origpc, control);
        }
        
        public Table getTable() {
            return table;
        }
    }
}