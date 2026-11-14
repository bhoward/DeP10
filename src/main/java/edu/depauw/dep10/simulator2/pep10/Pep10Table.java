package edu.depauw.dep10.simulator2.pep10;

import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.Operation;
import edu.depauw.dep10.simulator2.Table;
import edu.depauw.dep10.simulator2.pep10.Pep10Operation.OpCore;
import edu.depauw.dep10.simulator2.pep10.Pep10Operation.Unary;
import edu.depauw.dep10.util.Pair;

public class Pep10Table {
    public static Mode[] All = new Mode[] { Mode.I, Mode.D, Mode.N, Mode.S, Mode.SF, Mode.X, Mode.SX, Mode.SFX };
    public static Mode[] NotI = new Mode[] { Mode.D, Mode.N, Mode.S, Mode.SF, Mode.X, Mode.SX, Mode.SFX };
    public static Mode[] IX = new Mode[] { Mode.I, Mode.X1 };

    public static final Operation UNIMPLEMENTED = new Unary("UNIMP") {
        public void exec(Pep10Registers state) throws Fault {
            throw new Fault("Invalid instruction");
        }       
    };
    
    public static final Operation RET = new Unary("RET") {
        public void exec(Pep10Registers s) throws Fault {
            s.setPC(s.readW(s.getSP()));
            s.setSP(s.getSP().plus(2));
        }
    };

    public static final Operation SRET = new Unary("SRET") {
        public void exec(Pep10Registers s) throws Fault {
            var t = s.getSP();
            var saved = s.readB(t);
            s.setA(s.readW(t.plus(1)));
            s.setX(s.readW(t.plus(3)));
            s.setPC(s.readW(t.plus(5)));
            s.setSP(s.readW(t.plus(7)));
            s.writeW(Pep10Machine.INITIAL_SP, t.plus(12));
            s.setFlags(saved);
        }
    };

    public static final Operation MOVFLGA = new Unary("MOVFLGA") {
        public void exec(Pep10Registers s) {
            s.setA(s.getFlags());
        }
    };

    public static final Operation MOVAFLG = new Unary("MOVAFLG") {
        public void exec(Pep10Registers s) {
            s.setFlags(s.getA().lo());
        }
    };

    public static final Operation MOVSPA = new Unary("MOVSPA") {
        public void exec(Pep10Registers s) {
            s.setA(s.getSP());
        }
    };

    public static final Operation MOVASP = new Unary("MOVASP") {
        public void exec(Pep10Registers s) {
            s.setSP(s.getA());
        }
    };

    public static final Operation NOP = new Unary("NOP") {
        public void exec(Pep10Registers s) {
        }
    };

    public static final Operation NEGA = new Unary("NEGA") {
        public void exec(Pep10Registers s) {
            var a1 = s.getA();
            var sign1 = a1.isNegative();
            var zero1 = a1.isZero();

            var a2 = a1.negate();
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(sign1 && sign2);
            s.setC(zero1);
        }
    };

    public static final Operation NEGX = new Unary("NEGX") {
        public void exec(Pep10Registers s) {
            var x1 = s.getX();
            var sign1 = x1.isNegative();
            var zero1 = x1.isZero();

            var x2 = x1.negate();
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(sign1 && sign2);
            s.setC(zero1);
        }
    };

    public static final Operation ASLA = new Unary("ASLA") {
        public void exec(Pep10Registers s) {
            var a1 = s.getA();
            var sign1 = a1.isNegative();

            var a2 = a1.shiftLeft(false);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setC(sign1);
            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(sign1 != sign2);
        }
    };

    public static final Operation ASLX = new Unary("ASLX") {
        public void exec(Pep10Registers s) {
            var x1 = s.getX();
            var sign1 = x1.isNegative();

            var x2 = x1.shiftLeft(false);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setC(sign1);
            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(sign1 != sign2);
        }
    };

    public static final Operation ASRA = new Unary("ASRA") {
        public void exec(Pep10Registers s) {
            var a1 = s.getA();
            var sign1 = a1.isNegative();
            var carry = a1.bit(0);

            var a2 = a1.shiftRight(sign1);
            var zero2 = a2.isZero();

            s.setC(carry);
            s.setA(a2);
            s.setN(sign1);
            s.setZ(zero2);
            s.setV(false);
        }
    };

    public static final Operation ASRX = new Unary("ASRX") {
        public void exec(Pep10Registers s) {
            var x1 = s.getX();
            var sign1 = x1.isNegative();
            var carry = x1.bit(0);

            var x2 = x1.shiftRight(sign1);
            var zero2 = x2.isZero();

            s.setC(carry);
            s.setX(x2);
            s.setN(sign1);
            s.setZ(zero2);
            s.setV(false);
        }
    };

    public static final Operation NOTA = new Unary("NOTA") {
        public void exec(Pep10Registers s) {
            var a1 = s.getA();

            var a2 = a1.not();
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation NOTX = new Unary("NOTX") {
        public void exec(Pep10Registers s) {
            var x1 = s.getX();

            var x2 = x1.not();
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation ROLA = new Unary("ROLA") {
        public void exec(Pep10Registers s) {
            var a1 = s.getA();
            var carry = a1.isNegative();

            var a2 = a1.shiftLeft(s.getC());
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setC(carry);
            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation ROLX = new Unary("ROLX") {
        public void exec(Pep10Registers s) {
            var x1 = s.getX();
            var carry = x1.isNegative();

            var x2 = x1.shiftLeft(s.getC());
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setC(carry);
            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation RORA = new Unary("RORA") {
        public void exec(Pep10Registers s) {
            var a1 = s.getA();
            var carry = a1.bit(0);

            var a2 = a1.shiftRight(s.getC());
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setC(carry);
            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation RORX = new Unary("RORX") {
        public void exec(Pep10Registers s) {
            var x1 = s.getX();
            var carry = x1.bit(0);

            var x2 = x1.shiftRight(s.getC());
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setC(carry);
            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore BR = new OpCore("BR", IX) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            s.setPC(mode.resolveWord(s));
        }
    };

    public static final OpCore BRLE = new OpCore("BRLE", IX) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            if (s.getN() || s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRLT = new OpCore("BRLT", IX) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            if (s.getN()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BREQ = new OpCore("BREQ", IX) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            if (s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRNE = new OpCore("BRNE", IX) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            if (!s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRGE = new OpCore("BRGE", IX) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            if (!s.getN()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRGT = new OpCore("BRGT", IX) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            if (!s.getN() && !s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRV = new OpCore("BRV", IX) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            if (s.getV()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRC = new OpCore("BRC", IX) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            if (s.getC()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore CALL = new OpCore("CALL", IX) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            s.setSP(s.getSP().plus(-2));
            s.writeW(s.getSP(), s.getPC());
            s.setPC(mode.resolveWord(s));
        }
    };

    public static final OpCore SCALL = new OpCore("SCALL", All) {
        public void exec(Pep10Registers s, Mode mode) {
// TODO            s.trap(s.getOpCode(), s.getOperand()); // don't resolve the operand yet; the handler will do that
        }
    };

    public static final OpCore LDWA = new OpCore("LDWA", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);
            var sign = operand.isNegative();
            var zero = operand.isZero();

            s.setA(operand);
            s.setN(sign);
            s.setZ(zero);
        }
    };

    public static final OpCore LDWX = new OpCore("LDWX", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);
            var sign = operand.isNegative();
            var zero = operand.isZero();

            s.setX(operand);
            s.setN(sign);
            s.setZ(zero);
        }
    };

    public static final OpCore LDBA = new OpCore("LDBA", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveByte(s);
            var zero = operand.isZero();

            s.setA(operand);
            s.setN(false);
            s.setZ(zero);
        }
    };

    public static final OpCore LDBX = new OpCore("LDBX", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveByte(s);
            var zero = operand.isZero();

            s.setX(operand);
            s.setN(false);
            s.setZ(zero);
        }
    };

    public static final OpCore STWA = new OpCore("STWA", NotI) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var address = mode.getAddress(s);
            s.writeW(address, s.getA());
        }
    };

    public static final OpCore STWX = new OpCore("STWX", NotI) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            s.writeW(mode.getAddress(s), s.getX());
        }
    };

    public static final OpCore STBA = new OpCore("STBA", NotI) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            s.writeB(mode.getAddress(s), s.getA().lo());
        }
    };

    public static final OpCore STBX = new OpCore("STBX", NotI) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            s.writeB(mode.getAddress(s), s.getX().lo());
        }
    };

    public static final OpCore CPWA = new OpCore("CPWA", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();
            var sign1 = a1.isNegative();

            var op = operand.negate();
            var signo = operand.isNegative(); // sign of the operand itself, not of its negation

            var a2 = a1.plus(op);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();
            var overflow = (sign1 != signo) && (sign1 != sign2);
            var carry = !a1.lessThan(operand); // true if a1 >= operand, so no borrow

            s.setN(sign2 ^ overflow);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore CPWX = new OpCore("CPWX", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();
            var sign1 = x1.isNegative();

            var op = operand.negate();
            var signo = operand.isNegative(); // sign of the operand itself, not of its negation

            var x2 = x1.plus(op);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();
            var overflow = (sign1 != signo) && (sign1 != sign2);
            var carry = !x1.lessThan(operand); // true if x1 >= operand, so no borrow

            s.setN(sign2 ^ overflow);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore CPBA = new OpCore("CPBA", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveByte(s);

            var a1 = s.getA();

            var a2 = a1.minus(operand);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setN(sign2);
            s.setZ(zero2);
            s.setV(false);
            s.setC(false);
        }
    };

    public static final OpCore CPBX = new OpCore("CPBX", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveByte(s);

            var x1 = s.getX();

            var x2 = x1.minus(operand);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setN(sign2);
            s.setZ(zero2);
            s.setV(false);
            s.setC(false);
        }
    };

    public static final OpCore ADDA = new OpCore("ADDA", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();
            var sign1 = a1.isNegative();

            var signo = operand.isNegative();

            var a2 = a1.plus(operand);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();
            var overflow = (sign1 == signo) && (sign1 != sign2);
            var carry = a2.lessThan(a1) || a2.lessThan(operand);

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore ADDX = new OpCore("ADDX", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();
            var sign1 = x1.isNegative();

            var signo = operand.isNegative();

            var x2 = x1.plus(operand);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();
            var overflow = (sign1 == signo) && (sign1 != sign2);
            var carry = x2.lessThan(x1) || x2.lessThan(operand);

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore SUBA = new OpCore("SUBA", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();
            var sign1 = a1.isNegative();

            var op = operand.negate();
            var signo = operand.isNegative(); // sign of the operand itself, not of its negation

            var a2 = a1.plus(op);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();
            var overflow = (sign1 != signo) && (sign1 != sign2);
            var carry = !a1.lessThan(operand); // true if a1 >= operand, so no borrow

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore SUBX = new OpCore("SUBX", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();
            var sign1 = x1.isNegative();

            var op = operand.negate();
            var signo = operand.isNegative(); // sign of the operand itself, not of its negation

            var x2 = x1.plus(op);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();
            var overflow = (sign1 != signo) && (sign1 != sign2);
            var carry = !x1.lessThan(operand); // true if x1 >= operand, so no borrow

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore ANDA = new OpCore("ANDA", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();

            var a2 = a1.and(operand);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore ANDX = new OpCore("ANDX", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();

            var x2 = x1.and(operand);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore ORA = new OpCore("ORA", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();

            var a2 = a1.or(operand);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore ORX = new OpCore("ORX", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();

            var x2 = x1.or(operand);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore XORA = new OpCore("XORA", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();

            var a2 = a1.xor(operand);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore XORX = new OpCore("XORX", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();

            var x2 = x1.xor(operand);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore ADDSP = new OpCore("ADDSP", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var sp1 = s.getSP();

            var sp2 = sp1.plus(operand);

            s.setSP(sp2);
        }
    };

    public static final OpCore SUBSP = new OpCore("SUBSP", All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var sp1 = s.getSP();

            var op = operand.negate();

            var sp2 = sp1.plus(op);

            s.setSP(sp2);
        }
    };

    public static void installNonUnary(Table table, int code, OpCore op) {
        for (Mode mode : op.getModes()) {
            var index = code + mode.getOffset();
            if (index == 0) {
                throw new IllegalArgumentException("Zero opcodes are not supported");
            }
            table.install(index, new Pep10Operation.NonUnary(op, mode));
//            codes.put(new Pair<>(op.getMnemonic().toLowerCase(), mode.getSuffix().toLowerCase()), index);
        }
    }
    

    public static void installPep10Base(Table table) {
        table.install(1, RET);
        table.install(2, SRET);
        table.install(3, MOVFLGA);
        table.install(4, MOVAFLG);
        table.install(5, MOVSPA);
        table.install(6, MOVASP);
        table.install(7, NOP);

        table.install(24, NEGA);
        table.install(25, NEGX);
        table.install(26, ASLA);
        table.install(27, ASLX);
        table.install(28, ASRA);
        table.install(29, ASRX);
        table.install(30, NOTA);
        table.install(31, NOTX);
        table.install(32, ROLA);
        table.install(33, ROLX);
        table.install(34, RORA);
        table.install(35, RORX);

        installNonUnary(table, 36, BR);
        installNonUnary(table, 38, BRLE);
        installNonUnary(table, 40, BRLT);
        installNonUnary(table, 42, BREQ);
        installNonUnary(table, 44, BRNE);
        installNonUnary(table, 46, BRGE);
        installNonUnary(table, 48, BRGT);
        installNonUnary(table, 50, BRV);
        installNonUnary(table, 52, BRC);
        installNonUnary(table, 54, CALL);

        installNonUnary(table, 56, SCALL);
        installNonUnary(table, 64, ADDSP);
        installNonUnary(table, 72, SUBSP);

        installNonUnary(table, 80, ADDA);
        installNonUnary(table, 88, ADDX);
        installNonUnary(table, 96, SUBA);
        installNonUnary(table, 104, SUBX);
        installNonUnary(table, 112, ANDA);
        installNonUnary(table, 120, ANDX);
        installNonUnary(table, 128, ORA);
        installNonUnary(table, 136, ORX);
        installNonUnary(table, 144, XORA);
        installNonUnary(table, 152, XORX);
        installNonUnary(table, 160, CPWA);
        installNonUnary(table, 168, CPWX);
        installNonUnary(table, 176, CPBA);
        installNonUnary(table, 184, CPBX);
        installNonUnary(table, 192, LDWA);
        installNonUnary(table, 200, LDWX);
        installNonUnary(table, 208, LDBA);
        installNonUnary(table, 216, LDBX);
        installNonUnary(table, 224, STWA);
        installNonUnary(table, 232, STWX);
        installNonUnary(table, 240, STBA);
        installNonUnary(table, 248, STBX);
    }
}
