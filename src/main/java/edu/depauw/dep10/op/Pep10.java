package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

public class Pep10 {
    public static final Word TRAP_HANDLER_POINTER = Word.of(0xFFF7);
    public static final Word DISPATCHER_POINTER = Word.of(0xFFF9);
    public static final Word SYSTEM_STACK_POINTER = Word.of(0xFFFB);
    public static final Word CHARIN = Word.of(0xFFFD);
    public static final Word CHAROUT = Word.of(0xFFFE);
    public static final Word SHUTDOWN = Word.of(0xFFFF);
    
    public static final Table table = new Table();
    
    public static final Operation RET = new Operation.Unary("RET") {
        public void exec(State s) {
            s.setPC(s.mem2(s.getSP()));
            s.setSP(s.getSP().plus(2));
        }
    };

    public static final Operation SRET = new Operation.Unary("SRET") {
        public void exec(State s) {
            var t = s.getSP();
            s.setFlags(s.mem1(t));
            s.setA(s.mem2(t.plus(1)));
            s.setX(s.mem2(t.plus(3)));
            s.setPC(s.mem2(t.plus(5)));
            s.setSP(s.mem2(t.plus(7)));
            s.setMem2(SYSTEM_STACK_POINTER, t.plus(12));
        }
    };

    public static final Operation MOVFLGA = new Operation.Unary("MOVFLGA") {
        public void exec(State s) {
            s.setA(s.getFlags());
        }
    };

    public static final Operation MOVAFLG = new Operation.Unary("MOVAFLG") {
        public void exec(State s) {
            s.setFlags(s.getA().lo());
        }
    };

    public static final Operation MOVSPA = new Operation.Unary("MOVSPA") {
        public void exec(State s) {
            s.setA(s.getSP());
        }
    };

    public static final Operation MOVASP = new Operation.Unary("MOVASP") {
        public void exec(State s) {
            s.setSP(s.getA());
        }
    };

    public static final Operation NOP = new Operation.Unary("NOP") {
        public void exec(State s) {
        }
    };

    public static final Operation NEGA = new Operation.Unary("NEGA") {
        public void exec(State s) {
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

    public static final Operation NEGX = new Operation.Unary("NEGX") {
        public void exec(State s) {
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

    public static final Operation ASLA = new Operation.Unary("ASLA") {
        public void exec(State s) {
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

    public static final Operation ASLX = new Operation.Unary("ASLX") {
        public void exec(State s) {
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

    public static final Operation ASRA = new Operation.Unary("ASRA") {
        public void exec(State s) {
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

    public static final Operation ASRX = new Operation.Unary("ASRX") {
        public void exec(State s) {
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

    public static final Operation NOTA = new Operation.Unary("NOTA") {
        public void exec(State s) {
            var a1 = s.getA();

            var a2 = a1.not();
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation NOTX = new Operation.Unary("NOTX") {
        public void exec(State s) {
            var x1 = s.getX();

            var x2 = x1.not();
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation ROLA = new Operation.Unary("ROLA") {
        public void exec(State s) {
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

    public static final Operation ROLX = new Operation.Unary("ROLX") {
        public void exec(State s) {
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

    public static final Operation RORA = new Operation.Unary("RORA") {
        public void exec(State s) {
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

    public static final Operation RORX = new Operation.Unary("RORX") {
        public void exec(State s) {
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

    public static final OpCore BR = new OpCore("BR", Modes.IX) {
        public void exec(State s, Mode mode) {
            s.setPC(mode.resolveWord(s));
        }
    };

    public static final OpCore BRLE = new OpCore("BRLE", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (s.getN() || s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRLT = new OpCore("BRLT", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (s.getN()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BREQ = new OpCore("BREQ", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRNE = new OpCore("BRNE", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (!s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRGE = new OpCore("BRGE", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (!s.getN()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRGT = new OpCore("BRGT", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (!s.getN() && !s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRV = new OpCore("BRV", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (s.getV()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRC = new OpCore("BRC", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (s.getC()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore CALL = new OpCore("CALL", Modes.IX) {
        public void exec(State s, Mode mode) {
            s.setSP(s.getSP().plus(-2));
            s.setMem2(s.getSP(), s.getPC());
            s.setPC(mode.resolveWord(s));
        }
    };

    public static final OpCore SCALL = new OpCore("SCALL", Modes.All) {
        public void exec(State s, Mode mode) {
            var t = s.mem2(SYSTEM_STACK_POINTER);

            s.setMem2(t.plus(-2), s.getOperand()); // don't resolve yet; the handler will do that
            s.setMem1(t.plus(-3), s.getOpCode());
            s.setMem2(t.plus(-5), s.getSP());
            s.setMem2(t.plus(-7), s.getPC());
            s.setMem2(t.plus(-9), s.getX());
            s.setMem2(t.plus(-11), s.getA());
            s.setMem1(t.plus(-12), s.getFlags());

            s.setSP(t.plus(-12));
            s.setPC(s.mem2(TRAP_HANDLER_POINTER));
        }
    };

    public static final OpCore LDWA = new OpCore("LDWA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            var sign = operand.isNegative();
            var zero = operand.isZero();

            s.setA(operand);
            s.setN(sign);
            s.setZ(zero);
        }
    };

    public static final OpCore LDWX = new OpCore("LDWX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            var sign = operand.isNegative();
            var zero = operand.isZero();

            s.setX(operand);
            s.setN(sign);
            s.setZ(zero);
        }
    };

    public static final OpCore LDBA = new OpCore("LDBA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveByte(s);
            var zero = operand.isZero();

            s.setA(operand);
            s.setN(false);
            s.setZ(zero);
        }
    };

    public static final OpCore LDBX = new OpCore("LDBX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveByte(s);
            var zero = operand.isZero();

            s.setX(operand);
            s.setN(false);
            s.setZ(zero);
        }
    };

    public static final OpCore STWA = new OpCore("STWA", Modes.NotI) {
        public void exec(State s, Mode mode) {
            s.setMem2(mode.getAddress(s), s.getA());
        }
    };

    public static final OpCore STWX = new OpCore("STWX", Modes.NotI) {
        public void exec(State s, Mode mode) {
            s.setMem2(mode.getAddress(s), s.getX());
        }
    };

    public static final OpCore STBA = new OpCore("STBA", Modes.NotI) {
        public void exec(State s, Mode mode) {
            s.setMem1(mode.getAddress(s), s.getA().lo());
        }
    };

    public static final OpCore STBX = new OpCore("STBX", Modes.NotI) {
        public void exec(State s, Mode mode) {
            s.setMem1(mode.getAddress(s), s.getX().lo());
        }
    };

    public static final OpCore CPWA = new OpCore("CPWA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var a1 = s.getA();
            var sign1 = a1.isNegative();

            var op = operand.negate();
            var signo = op.isNegative();

            var a2 = a1.plus(op);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();
            var overflow = (sign1 == signo) && (sign1 != sign2);
            var carry = a2.lessThan(a1) || a2.lessThan(op);

            s.setN(sign2 ^ overflow);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore CPWX = new OpCore("CPWX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var x1 = s.getX();
            var sign1 = x1.isNegative();

            var op = operand.negate();
            var signo = op.isNegative();

            var x2 = x1.plus(op);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();
            var overflow = (sign1 == signo) && (sign1 != sign2);
            var carry = x2.lessThan(x1) || x2.lessThan(op);

            s.setN(sign2 ^ overflow);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore CPBA = new OpCore("CPBA", Modes.All) {
        public void exec(State s, Mode mode) {
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

    public static final OpCore CPBX = new OpCore("CPBX", Modes.All) {
        public void exec(State s, Mode mode) {
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

    public static final OpCore ADDA = new OpCore("ADDA", Modes.All) {
        public void exec(State s, Mode mode) {
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

    public static final OpCore ADDX = new OpCore("ADDX", Modes.All) {
        public void exec(State s, Mode mode) {
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

    public static final OpCore SUBA = new OpCore("SUBA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var a1 = s.getA();
            var sign1 = a1.isNegative();

            var op = operand.negate();
            var signo = op.isNegative();

            var a2 = a1.plus(op);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();
            var overflow = (sign1 == signo) && (sign1 != sign2);
            var carry = a2.lessThan(a1) || a2.lessThan(op);

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore SUBX = new OpCore("SUBX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var x1 = s.getX();
            var sign1 = x1.isNegative();

            var op = operand.negate();
            var signo = op.isNegative();

            var x2 = x1.plus(op);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();
            var overflow = (sign1 == signo) && (sign1 != sign2);
            var carry = x2.lessThan(x1) || x2.lessThan(op);

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore ANDA = new OpCore("ANDA", Modes.All) {
        public void exec(State s, Mode mode) {
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

    public static final OpCore ANDX = new OpCore("ANDX", Modes.All) {
        public void exec(State s, Mode mode) {
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

    public static final OpCore ORA = new OpCore("ORA", Modes.All) {
        public void exec(State s, Mode mode) {
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

    public static final OpCore ORX = new OpCore("ORX", Modes.All) {
        public void exec(State s, Mode mode) {
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

    public static final OpCore XORA = new OpCore("XORA", Modes.All) {
        public void exec(State s, Mode mode) {
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

    public static final OpCore XORX = new OpCore("XORX", Modes.All) {
        public void exec(State s, Mode mode) {
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

    public static final OpCore ADDSP = new OpCore("ADDSP", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var sp1 = s.getSP();

            var sp2 = sp1.plus(operand);

            s.setSP(sp2);
        }
    };

    public static final OpCore SUBSP = new OpCore("SUBSP", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var sp1 = s.getSP();

            var op = operand.negate();

            var sp2 = sp1.plus(op);

            s.setSP(sp2);
        }
    };
    
    public static final OpCore MULA = new OpCore("MULA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var a = s.getA().value();
            var op = operand.value();
            var product = a * op;
            var low_bits = Word.of(product);

            s.setA(low_bits);

            // N: set if product is <0, cleared otherwise
            s.setN(product < 0);

            // Z: set if product is 0, cleared otherwise
            s.setZ(product == 0);
            
            // V: overflow value needs to be cleared, so don't set at all (?)
            s.setV(product == 0);

            // C: Carry will only be set if result is less than -2^15 or greater than 2^15 - 1
            s.setC(low_bits.value() != product);
        }
    };

    public static final OpCore MULHA = new OpCore("MULHA", Modes.All){
        public void exec(State s, Mode mode){
            var operand = mode.resolveWord(s);
            
            // update logic for signed * signed (0x10000 - 65536 states for two's complement)
            var a = s.getA().isNegative() ? s.getA().value() - 0x10000: s.getA().value();
            var op = operand.isNegative() ? operand.value() - 0x10000 : operand.value();

            var product = a * op;
            var high_bits = Word.of(product >>> 16); 

            s.setA(high_bits);

            s.setN(high_bits.isNegative());
            s.setZ(high_bits.isZero());
            // check these two: do we need them set like MULA?
            s.setV(false);
            s.setC(false);
        }
    };

    public static final OpCore DIV = new OpCore("DIV", Modes.All){
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            // Jess Notes
            var divisor = operand.value();
            if (divisor == 0) {
                s.setC(true);
                s.setV(true);
                return;
            }
            // X is high_bits, A is low_bits
            // A is quotient, X is remainder

            var high_bits = s.getX().value();
            var low_bits = s.getA().value();
            int dividend = (high_bits << 16) | low_bits;
            int quotient  = dividend / divisor;
            int remainder = dividend % divisor;
            s.setA(Word.of(quotient));
            s.setX(Word.of(remainder));

            // flags
            // N: if quotient <0, cleared otherwise
            s.setN(quotient < 0);
            // Z: if quotient is 0, cleared otherwise
            s.setZ(quotient == 0);
            // V: if source = 0 or...?
            s.setV(s.getA().value() != quotient);
            // C: set if divide 0 attempted, clear otherwise
            s.setC(false);
        }
    };

    
    static {
        table.install(1, RET);
        table.install(2, SRET);
        table.install(3, MOVFLGA);
        table.install(4, MOVAFLG);
        table.install(5, MOVSPA);
        table.install(6, MOVASP);
        table.install(7, NOP);
        
        var MulDiv = new Table();
        MulDiv.install(8, MULA); // NOTE opcode 0 should be unimplemented in any table, except perhaps as a prefix
        MulDiv.install(16, MULHA);
        //MulDiv.install(16, DIV);

        table.install(8,  MulDiv);
        
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
        
        table.install(36, BR);
        table.install(38, BRLE);
        table.install(40, BRLT);
        table.install(42, BREQ);
        table.install(44, BRNE);
        table.install(46, BRGE);
        table.install(48, BRGT);
        table.install(50, BRV);
        table.install(52, BRC);
        table.install(54, CALL);

        table.install(56, SCALL);
        table.install(64, ADDSP);
        table.install(72, SUBSP);
        
        table.install(80, ADDA);
        table.install(88, ADDX);
        table.install(96, SUBA);
        table.install(104, SUBX);
        table.install(112, ANDA);
        table.install(120, ANDX);
        table.install(128, ORA);
        table.install(136, ORX);
        table.install(144, XORA);
        table.install(152, XORX);
        table.install(160, CPWA);
        table.install(168, CPWX);
        table.install(176, CPBA);
        table.install(184, CPBX);
        table.install(192, LDWA);
        table.install(200, LDWX);
        table.install(208, LDBA);
        table.install(216, LDBX);
        table.install(224, STWA);
        table.install(232, STWX);
        table.install(240, STBA);
        table.install(248, STBX);
    } 
}
