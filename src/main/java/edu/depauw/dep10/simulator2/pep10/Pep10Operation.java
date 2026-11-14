package edu.depauw.dep10.simulator2.pep10;

import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.Operation;
import edu.depauw.dep10.simulator2.State;

public interface Pep10Operation extends Operation {
    default void execute(State state) throws Fault {
        exec((Pep10Registers) state);
    }

    void exec(Pep10Registers state) throws Fault;

    String getMnemonic();

    boolean hasOperand();

    public abstract class Unary implements Pep10Operation {
        private String mnemonic;

        public Unary(String mnemonic) {
            this.mnemonic = mnemonic;
        }

        public String getMnemonic() {
            return mnemonic;
        }

        @Override
        public String toString() {
            return String.format("%-7s", mnemonic);
        }

        public boolean hasOperand() {
            return false;
        }
    }

    public abstract class OpCore {
        private String mnemonic;
        private Mode[] modes;

        public OpCore(String mnemonic, Mode[] modes) {
            this.mnemonic = mnemonic;
            this.modes = modes;
        }

        public abstract void exec(Pep10Registers state, Mode mode) throws Fault;

        public String getMnemonic() {
            return mnemonic;
        }

        public Mode[] getModes() {
            return modes;
        }
    }

    public class NonUnary implements Pep10Operation {
        private OpCore op;
        private Mode mode;

        public NonUnary(OpCore op, Mode mode) {
            this.op = op;
            this.mode = mode;
        }

        public String getMnemonic() {
            return op.getMnemonic();
        }

        public void exec(Pep10Registers s) throws Fault {
            var pc = s.getPC();
            var operand = s.fetchW(pc); // operand specifier: an instruction fetch
            s.setEA(pc); // effective address in case of immediate mode
            s.setPC(pc.plus(2));
            s.setOperand(operand);
            op.exec(s, mode);
        }

        @Override
        public String toString() {
            return String.format("%-7s", op.getMnemonic() + "," + mode.getSuffix());
        }

        public boolean hasOperand() {
            return true;
        }
    }
}