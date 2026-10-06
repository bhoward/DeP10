package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

/**
 * Utility extension table (prefix 0000 1001): stack, register swap and address
 * instructions. Unary instructions use opcodes 1 to 15; two-register families
 * take 16 codes each, with the A form at 16k and the X form at 16k + 8.
 */
public class Dep10Util {
    public static final Table table = new Table();

    public static final Operation PUSHX = new Operation.Unary("PUSHX") {
        public void exec(State s) {
            s.setSP(s.getSP().plus(-2));
            s.setMem2(s.getSP(), s.getX());
        }
    };

    public static final Operation PUSHA = new Operation.Unary("PUSHA") {
        public void exec(State s) {
            s.setSP(s.getSP().plus(-2));
            s.setMem2(s.getSP(), s.getA());
        }
    };

    public static final Operation POPX = new Operation.Unary("POPX") {
        public void exec(State s) {
            s.setX(s.mem2(s.getSP()));
            s.setSP(s.getSP().plus(2));
        }
    };

    public static final Operation POPA = new Operation.Unary("POPA") {
        public void exec(State s) {
            s.setA(s.mem2(s.getSP()));
            s.setSP(s.getSP().plus(2));
        }
    };
    
    public static final Operation SWAPAX = new Operation.Unary("SWAPAX") {
        public void exec(State s) {
            var a = s.getA();
            var x = s.getX();
            s.setA(x);
            s.setX(a);
        }
    };
    
    public static final Operation SWAPHA = new Operation.Unary("SWAPHA") {
        public void exec(State s) {
            var h = s.getH();
            var a = s.getA();
            s.setH(a);
            s.setA(h);
        }
    };
    
    public static final Operation SWAPHX = new Operation.Unary("SWAPHX") {
        public void exec(State s) {
            var h = s.getH();
            var x = s.getX();
            s.setH(x);
            s.setX(h);
        }
    };
    
    private static void setNZ(State s, Word w) {
        s.setN(w.isNegative());
        s.setZ(w.isZero());
    }

    /** A <- effective address of Operand (no memory access). Not valid with immediate mode. N and Z from A. */
    public static final OpCore LEAA = new OpCore("LEAA", Modes.NotI) {
        public void exec(State s, Mode mode) {
            var a = mode.getAddress(s);
            s.setA(a);
            setNZ(s, a);
        }
    };

    public static final OpCore LEAX = new OpCore("LEAX", Modes.NotI) {
        public void exec(State s, Mode mode) {
            var x = mode.getAddress(s);
            s.setX(x);
            setNZ(s, x);
        }
    };

    /** Exchange A with the word at the operand address in one step. N and Z from the new A. */
    public static final OpCore SWAPA = new OpCore("SWAPA", Modes.NotI) {
        public void exec(State s, Mode mode) {
            var addr = mode.getAddress(s);
            var old = s.mem2(addr);
            s.setMem2(addr, s.getA());
            s.setA(old);
            setNZ(s, old);
        }
    };

    public static final OpCore SWAPX = new OpCore("SWAPX", Modes.NotI) {
        public void exec(State s, Mode mode) {
            var addr = mode.getAddress(s);
            var old = s.mem2(addr);
            s.setMem2(addr, s.getX());
            s.setX(old);
            setNZ(s, old);
        }
    };

    static {
        // unary
        table.install(1, SWAPAX);
        table.install(2, SWAPHA);
        table.install(3, SWAPHX);
        table.install(4, PUSHA);
        table.install(5, PUSHX);
        table.install(6, POPA);
        table.install(7, POPX);
        // A form at 16k, X form at 16k + 8
        table.install(16, LEAA);
        table.install(24, LEAX);
        table.install(32, SWAPA);
        table.install(40, SWAPX);
    }
}
