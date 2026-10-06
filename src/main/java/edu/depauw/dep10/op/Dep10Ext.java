package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

/**
 * Extension table (prefix 0000 1010): instructions requested for compiler
 * support (TSTr, SXBr, SXWr, ADCr, SBCr, LEAr, SWAPr).
 */
public class Dep10Ext {
    public static final Table table = new Table();

    private static void setNZ(State s, Word w) {
        s.setN(w.isNegative());
        s.setZ(w.isZero());
    }

    private static Word signExtendByte(Word w) {
        return Word.of((byte) w.value());
    }

    /** Test A: same flags as CPWA 0,i. N and Z from A, V cleared, C set (no borrow). */
    public static final Operation TSTA = new Operation.Unary("TSTA") {
        public void exec(State s) {
            setNZ(s, s.getA());
            s.setV(false);
            s.setC(true);
        }
    };

    /** Test X: same flags as CPWX 0,i. */
    public static final Operation TSTX = new Operation.Unary("TSTX") {
        public void exec(State s) {
            setNZ(s, s.getX());
            s.setV(false);
            s.setC(true);
        }
    };

    /** Sign-extend the low byte of A into its high byte. N and Z from the result; V and C unchanged. */
    public static final Operation SXBA = new Operation.Unary("SXBA") {
        public void exec(State s) {
            var a = signExtendByte(s.getA());
            s.setA(a);
            setNZ(s, a);
        }
    };

    public static final Operation SXBX = new Operation.Unary("SXBX") {
        public void exec(State s) {
            var x = signExtendByte(s.getX());
            s.setX(x);
            setNZ(s, x);
        }
    };

    /**
     * Sign-extend A into H, so that H:A is the 32-bit signed value of A.
     * N is the sign of the 32-bit value and Z is true when it is zero; V and C unchanged.
     */
    public static final Operation SXWA = new Operation.Unary("SXWA") {
        public void exec(State s) {
            var a = s.getA();
            s.setH(Word.of(a.isNegative() ? 0xFFFF : 0));
            setNZ(s, a);
        }
    };

    /** Sign-extend X into H, so that H:X is the 32-bit signed value of X. */
    public static final Operation SXWX = new Operation.Unary("SXWX") {
        public void exec(State s) {
            var x = s.getX();
            s.setH(Word.of(x.isNegative() ? 0xFFFF : 0));
            setNZ(s, x);
        }
    };

    private static void add(State s, Word r1, Word operand, boolean carryIn, boolean toA) {
        var sum = r1.value() + operand.value() + (carryIn ? 1 : 0);
        var r2 = Word.of(sum);
        var sign1 = r1.isNegative();
        var signo = operand.isNegative();
        var sign2 = r2.isNegative();
        if (toA) {
            s.setA(r2);
        } else {
            s.setX(r2);
        }
        s.setN(sign2);
        s.setZ(r2.isZero());
        s.setV((sign1 == signo) && (sign1 != sign2));
        s.setC(sum > 0xFFFF);
    }

    // As with SUB and CPW, C means "no borrow", so SBC computes r + ~operand + C.
    private static void sub(State s, Word r1, Word operand, boolean carryIn, boolean toA) {
        var sum = r1.value() + (~operand.value() & 0xFFFF) + (carryIn ? 1 : 0);
        var r2 = Word.of(sum);
        var sign1 = r1.isNegative();
        var signNotO = !operand.isNegative();
        var sign2 = r2.isNegative();
        if (toA) {
            s.setA(r2);
        } else {
            s.setX(r2);
        }
        s.setN(sign2);
        s.setZ(r2.isZero());
        s.setV((sign1 == signNotO) && (sign1 != sign2));
        s.setC(sum > 0xFFFF);
    }

    /** A <- A + Operand + C. Flags as ADDA. Z reflects only this word's result. */
    public static final OpCore ADCA = new OpCore("ADCA", Modes.All) {
        public void exec(State s, Mode mode) {
            add(s, s.getA(), mode.resolveWord(s), s.getC(), true);
        }
    };

    public static final OpCore ADCX = new OpCore("ADCX", Modes.All) {
        public void exec(State s, Mode mode) {
            add(s, s.getX(), mode.resolveWord(s), s.getC(), false);
        }
    };

    /** A <- A - Operand - (1 - C). As with SUBA, C set means no borrow. */
    public static final OpCore SBCA = new OpCore("SBCA", Modes.All) {
        public void exec(State s, Mode mode) {
            sub(s, s.getA(), mode.resolveWord(s), s.getC(), true);
        }
    };

    public static final OpCore SBCX = new OpCore("SBCX", Modes.All) {
        public void exec(State s, Mode mode) {
            sub(s, s.getX(), mode.resolveWord(s), s.getC(), false);
        }
    };

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
        table.install(1, TSTA);
        table.install(2, TSTX);
        table.install(3, SXBA);
        table.install(4, SXBX);
        table.install(5, SXWA);
        table.install(6, SXWX);
        table.install(8, ADCA);
        table.install(16, ADCX);
        table.install(24, SBCA);
        table.install(32, SBCX);
        table.install(40, LEAA);
        table.install(48, LEAX);
        table.install(56, SWAPA);
        table.install(64, SWAPX);
    }
}
