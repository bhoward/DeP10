package edu.depauw.dep10.simulator2.dep10;

import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.Operation;
import edu.depauw.dep10.simulator2.State;

/**
 * An instruction that needs the DeP10 registers (such as H). Instructions that
 * only use the Pep/10 registers can be written as ordinary
 * {@link edu.depauw.dep10.simulator2.pep10.Pep10Operation}s.
 */
public interface Dep10Operation extends Operation {
    default void execute(State state) throws Fault {
        exec((Dep10Registers) state);
    }

    void exec(Dep10Registers state) throws Fault;

    public abstract class Unary implements Dep10Operation {
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
}
