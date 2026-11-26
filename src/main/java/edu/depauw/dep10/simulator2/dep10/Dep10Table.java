package edu.depauw.dep10.simulator2.dep10;

import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.Operation;
import edu.depauw.dep10.simulator2.Table;
import edu.depauw.dep10.simulator2.pep10.Mode;
import edu.depauw.dep10.simulator2.pep10.Pep10Operation;
import edu.depauw.dep10.simulator2.pep10.Pep10Operation.OpCore;
import edu.depauw.dep10.simulator2.pep10.Pep10Operation.Unary;
import edu.depauw.dep10.simulator2.pep10.Pep10Registers;
import edu.depauw.dep10.simulator2.pep10.Pep10Table;
import edu.depauw.dep10.util.Word;

/**
 * The DeP10 instruction set: the standard Pep/10 instructions, plus two-byte
 * extensions behind the prefix opcodes 0x08 (arithmetic) and 0x09 (utility).
 */
public class Dep10Table {
    /** Prefix of the arithmetic extension table (not yet installed). */
    public static final int ARITH_PREFIX = 0x08;

    /** Prefix of the utility extension table. */
    public static final int UTIL_PREFIX = 0x09;

    public static final Operation SWAPAX = new Unary("SWAPAX") {
        public void exec(Pep10Registers s) {
            var a = s.getA();
            s.setA(s.getX());
            s.setX(a);
        }
    };

    public static final Operation SWAPHA = new Dep10Operation.Unary("SWAPHA") {
        public void exec(Dep10Registers s) {
            var h = s.getH();
            s.setH(s.getA());
            s.setA(h);
        }
    };

    public static final Operation SWAPHX = new Dep10Operation.Unary("SWAPHX") {
        public void exec(Dep10Registers s) {
            var h = s.getH();
            s.setH(s.getX());
            s.setX(h);
        }
    };

    // The new SP is written only after the memory access succeeds, so that a
    // fault leaves the registers as they were.
    public static final Operation PUSHA = new Unary("PUSHA") {
        public void exec(Pep10Registers s) throws Fault {
            var sp = s.getSP().plus(-2);
            s.writeW(sp, s.getA());
            s.setSP(sp);
        }
    };

    public static final Operation PUSHX = new Unary("PUSHX") {
        public void exec(Pep10Registers s) throws Fault {
            var sp = s.getSP().plus(-2);
            s.writeW(sp, s.getX());
            s.setSP(sp);
        }
    };

    public static final Operation POPA = new Unary("POPA") {
        public void exec(Pep10Registers s) throws Fault {
            var sp = s.getSP();
            s.setA(s.readW(sp));
            s.setSP(sp.plus(2));
        }
    };

    public static final Operation POPX = new Unary("POPX") {
        public void exec(Pep10Registers s) throws Fault {
            var sp = s.getSP();
            s.setX(s.readW(sp));
            s.setSP(sp.plus(2));
        }
    };

    private static void setNZ(Pep10Registers s, Word w) {
        s.setN(w.isNegative());
        s.setZ(w.isZero());
    }

    /** A <- effective address of the operand (no memory access). N and Z from A. */
    public static final OpCore LEAA = new OpCore("LEAA", Pep10Table.NotI) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var a = mode.getAddress(s);
            s.setA(a);
            setNZ(s, a);
        }
    };

    public static final OpCore LEAX = new OpCore("LEAX", Pep10Table.NotI) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var x = mode.getAddress(s);
            s.setX(x);
            setNZ(s, x);
        }
    };

    /** Exchange A with the word at the operand address. N and Z from the new A. */
    public static final OpCore SWAPA = new OpCore("SWAPA", Pep10Table.NotI) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var addr = mode.getAddress(s);
            var old = s.readW(addr);
            s.writeW(addr, s.getA());
            s.setA(old);
            setNZ(s, old);
        }
    };

    public static final OpCore SWAPX = new OpCore("SWAPX", Pep10Table.NotI) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var addr = mode.getAddress(s);
            var old = s.readW(addr);
            s.writeW(addr, s.getX());
            s.setX(old);
            setNZ(s, old);
        }
    };

    /**
     * Branch if P is set (supervisor mode on the fancy machine; never taken on the
     * plain machine).
     */
    public static final OpCore BRP = new OpCore("BRP", Pep10Table.IX) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            if ((s.getFlags().value() & 0x80) != 0) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    /**
     * Build the utility extension table. Unary instructions use opcodes 1 to 15;
     * the two-register families take 16 codes each, with the A form at 16k and the
     * X form at 16k + 8.
     * 
     * @return the table
     */
    public static Table utilTable() {
        var table = new Table(Pep10Table.UNIMPLEMENTED);

        Pep10Table.installUnary(table, 1, SWAPAX);
        Pep10Table.installUnary(table, 2, SWAPHA);
        Pep10Table.installUnary(table, 3, SWAPHX);
        Pep10Table.installUnary(table, 4, PUSHA);
        Pep10Table.installUnary(table, 5, PUSHX);
        Pep10Table.installUnary(table, 6, POPA);
        Pep10Table.installUnary(table, 7, POPX);

        Pep10Table.installNonUnary(table, 16, LEAA);
        Pep10Table.installNonUnary(table, 24, LEAX);
        Pep10Table.installNonUnary(table, 32, SWAPA);
        Pep10Table.installNonUnary(table, 40, SWAPX);
        Pep10Table.installNonUnary(table, 48, BRP);

        return table;
    }

    /**
     * Load the DeP10 instruction set into the table: the standard Pep/10 base and
     * the extension tables.
     * 
     * @param table
     */
    public static void installDep10(Table table) {
        Pep10Table.installPep10Base(table);
        Pep10Table.installPrefix(table, UTIL_PREFIX, utilTable());
    }
}
