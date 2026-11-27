package edu.depauw.dep10.simulator2.dep10;

import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.Operation;
import edu.depauw.dep10.simulator2.Table;
import edu.depauw.dep10.simulator2.dep10.Dep10Operation.Dep10OpCore;
import edu.depauw.dep10.simulator2.pep10.Mode;
import edu.depauw.dep10.simulator2.pep10.Pep10Operation.OpCore;
import edu.depauw.dep10.simulator2.pep10.Pep10Operation.Unary;
import edu.depauw.dep10.simulator2.pep10.Pep10Registers;
import edu.depauw.dep10.simulator2.pep10.Pep10Table;
import edu.depauw.dep10.util.Word;

/**
 * Arithmetic extension table (prefix 0000 1000). Unary instructions use opcodes
 * 1 to 15; each two-register family takes 16 codes, with the A form at 16k and
 * the X form at 16k + 8, so MULr is 0001rxxx, DIVr is 0010rxxx, and so on.
 */
public class Dep10Arith {
    /**
     * 16-bit (A) times 16-bit (operand) multiplication
     * leaves 16-bit (truncated) product in A
     * Z is true if product is zero
     * N is true if product is negative
     * C is true if full product does not fit in 16 bits (< -2^15 or >= +2^15) 
     * V is false
     */
    public static final OpCore MULA = new OpCore("MULA", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var a = s.getA().signedValue();
            var op = operand.signedValue();

            var product = a * op;
            var low_bits = Word.of(product);

            s.setA(low_bits);
            s.setN(low_bits.isNegative());
            s.setZ(low_bits.isZero());
            s.setV(false);
            s.setC(low_bits.value() != product);
        }
    };

    /**
     * 16-bit (X) times 16-bit (operand) multiplication
     * leaves 16-bit (truncated) product in X
     * Z is true if product is zero
     * N is true if product is negative
     * C is true if full product does not fit in 16 bits (< -2^15 or >= +2^15) 
     * V is false
     */
    public static final OpCore MULX = new OpCore("MULX", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var x = s.getX().signedValue();
            var op = operand.signedValue();

            var product = x * op;
            var low_bits = Word.of(product);

            s.setX(low_bits);
            s.setN(low_bits.isNegative());
            s.setZ(low_bits.isZero());
            s.setV(false);
            s.setC(low_bits.value() != product);
        }
    };

    /**
     * 16-bit (A) by 16-bit (operand) signed integer division
     * leaves 16-bit (truncated) quotient in A
     * Z is true if quotient is zero
     * N is true if quotient is negative
     * C is true if divide by zero, else false
     * V is true if divide by zero or result overflows 16 bits (only occurs for -32768 / -1)
     * On divide by zero, quotient will be zero
     */
    public static final OpCore DIVA = new OpCore("DIVA", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var a = s.getA().signedValue();
            var op = operand.signedValue();

            if (op == 0) {
                s.setA(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setC(true);
                s.setV(true);
                return;
            }

            var quotient = a / op;

            s.setA(Word.of(quotient));
            s.setN(quotient < 0);
            s.setZ(quotient == 0);
            s.setV(a == Short.MIN_VALUE && op == -1);
            s.setC(false);
        }
    };

    /**
     * 16-bit (X) by 16-bit (operand) signed integer division
     * leaves 16-bit (truncated) quotient in X
     * Z is true if quotient is zero
     * N is true if quotient is negative
     * C is true if divide by zero, else false
     * V is true if divide by zero or result overflows 16 bits (only occurs for -32768 / -1)
     * On divide by zero, quotient will be zero
     */
    public static final OpCore DIVX = new OpCore("DIVX", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var x = s.getX().signedValue();
            var op = operand.signedValue();

            if (op == 0) {
                s.setX(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setC(true);
                s.setV(true);
                return;
            }

            var quotient = x / op;

            s.setX(Word.of(quotient));
            s.setN(quotient < 0);
            s.setZ(quotient == 0);
            s.setV(x == Short.MIN_VALUE && op == -1);
            s.setC(false);
        }
    };

    /**
     * 16-bit (A) by 16-bit (operand) signed integer division
     * leaves 16-bit remainder in A
     * Z is true if remainder is zero
     * N is true if remainder is negative
     * C is true if divide by zero, else false
     * V is true if divide by zero, else false
     * On divide by zero, remainder will be zero
     */
    public static final OpCore MODA = new OpCore("MODA", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var a = s.getA().signedValue();
            var op = operand.signedValue();

            if (op == 0) {
                s.setA(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setC(true);
                s.setV(true);
                return;
            }

            var remainder = a % op;

            s.setA(Word.of(remainder));
            s.setN(remainder < 0);
            s.setZ(remainder == 0);
            s.setV(false);
            s.setC(false);
        }
    };

    /**
     * 16-bit (X) by 16-bit (operand) signed integer division
     * leaves 16-bit remainder in X
     * Z is true if remainder is zero
     * N is true if remainder is negative
     * C is true if divide by zero, else false
     * V is true if divide by zero, else false
     * On divide by zero, remainder will be zero
     */
    public static final OpCore MODX = new OpCore("MODX", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var x = s.getX().signedValue();
            var op = operand.signedValue();

            if (op == 0) {
                s.setX(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setC(true);
                s.setV(true);
                return;
            }

            var remainder = x % op;

            s.setX(Word.of(remainder));
            s.setN(remainder < 0);
            s.setZ(remainder == 0);
            s.setV(false);
            s.setC(false);
        }
    };

    /**
     * 16-bit (A) by 16-bit (operand) unsigned integer division
     * leaves 16-bit quotient in A
     * Z is true if quotient is zero
     * N is true if quotient is negative (as a signed 16-bit integer)
     * C is true if divide by zero, else false
     * V is true if divide by zero, else false
     * On divide by zero, quotient will be zero
     */
    public static final OpCore UDIVA = new OpCore("UDIVA", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var a = s.getA().value();
            var op = operand.value();

            if (op == 0) {
                s.setA(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setC(true);
                s.setV(true);
                return;
            }

            var quotient = a / op;
            var result = Word.of(quotient);

            s.setA(result);
            s.setN(result.isNegative());
            s.setZ(quotient == 0);
            s.setV(false);
            s.setC(false);
        }
    };

    /**
     * 16-bit (X) by 16-bit (operand) unsigned integer division
     * leaves 16-bit quotient in X
     * Z is true if quotient is zero
     * N is true if quotient is negative (as a signed 16-bit integer)
     * C is true if divide by zero, else false
     * V is true if divide by zero, else false
     * On divide by zero, quotient will be zero
     */
    public static final OpCore UDIVX = new OpCore("UDIVX", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var x = s.getX().value();
            var op = operand.value();

            if (op == 0) {
                s.setX(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setC(true);
                s.setV(true);
                return;
            }

            var quotient = x / op;
            var result = Word.of(quotient);

            s.setX(result);
            s.setN(result.isNegative());
            s.setZ(quotient == 0);
            s.setV(false);
            s.setC(false);
        }
    };

    /**
     * 16-bit (A) by 16-bit (operand) unsigned integer division
     * leaves 16-bit remainder in A
     * Z is true if remainder is zero
     * N is true if remainder is negative (as a signed 16-bit integer)
     * C is true if divide by zero, else false
     * V is true if divide by zero, else false
     * On divide by zero, remainder will be zero
     */
    public static final OpCore UMODA = new OpCore("UMODA", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var a = s.getA().value();
            var op = operand.value();

            if (op == 0) {
                s.setA(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setC(true);
                s.setV(true);
                return;
            }

            var remainder = a % op;
            var result = Word.of(remainder);

            s.setA(result);
            s.setN(result.isNegative());
            s.setZ(remainder == 0);
            s.setV(false);
            s.setC(false);
        }
    };

    /**
     * 16-bit (X) by 16-bit (operand) unsigned integer division
     * leaves 16-bit remainder in X
     * Z is true if remainder is zero
     * N is true if remainder is negative (as a signed 16-bit integer)
     * C is true if divide by zero, else false
     * V is true if divide by zero, else false
     * On divide by zero, remainder will be zero
     */
    public static final OpCore UMODX = new OpCore("UMODX", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);

            var x = s.getX().value();
            var op = operand.value();

            if (op == 0) {
                s.setX(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setC(true);
                s.setV(true);
                return;
            }

            var remainder = x % op;
            var result = Word.of(remainder);

            s.setX(result);
            s.setN(result.isNegative());
            s.setZ(remainder == 0);
            s.setV(false);
            s.setC(false);
        }
    };

    /**
     * 16-bit (A) times 16-bit (operand) signed integer multiply, 32-bit result
     * high word will be in H register, low word in A
     * Z is true if full product is zero
     * N is true if full product is negative
     * C is true if full product does not fit in 16 bits (that is, if H is not just
     *   the sign-extend of A)
     * V is not affected
     */
    public static final OpCore SMULA = new Dep10OpCore("SMULA", Pep10Table.All) {
        public void exec(Dep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);
            
            var a = s.getA().signedValue();
            var op = operand.signedValue();

            var product = a * op;
            var low_bits = Word.of(product);
            var high_bits = Word.of(product >>> 16);

            s.setA(low_bits);
            s.setH(high_bits);

            s.setN(high_bits.isNegative());
            s.setZ(low_bits.isZero() && high_bits.isZero());
            s.setC(low_bits.signedValue() != product);
        }
    };

    /**
     * 16-bit (X) times 16-bit (operand) signed integer multiply, 32-bit result
     * high word will be in H register, low word in X
     * Z is true if full product is zero
     * N is true if full product is negative
     * C is true if full product does not fit in 16 bits (that is, if H is not just
     *   the sign-extend of X)
     * V is not affected
     */
    public static final OpCore SMULX = new Dep10OpCore("SMULX", Pep10Table.All) {
        public void exec(Dep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);
            
            var x = s.getX().signedValue();
            var op = operand.signedValue();

            var product = x * op;
            var low_bits = Word.of(product);
            var high_bits = Word.of(product >>> 16);

            s.setX(low_bits);
            s.setH(high_bits);

            s.setN(high_bits.isNegative());
            s.setZ(low_bits.isZero() && high_bits.isZero());
            s.setC(low_bits.signedValue() != product);
        }
    };

    /**
     * 16-bit (A) times 16-bit (operand) unsigned integer multiply, 32-bit result
     * high word will be in H register, low word in A
     * Z is true if full product is zero
     * N is true if full product is negative (as a signed 32-bit number)
     * C is true if full product does not fit in 16 bits (that is, if H is not zero)
     * V is not affected
     */
    public static final OpCore UMULA = new Dep10OpCore("UMULA", Pep10Table.All) {
        public void exec(Dep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);
            
            var a = s.getA().value();
            var op = operand.value();

            var product = a * op;
            var low_bits = Word.of(product);
            var high_bits = Word.of(product >> 16);

            s.setA(low_bits);
            s.setH(high_bits);

            s.setN(high_bits.isNegative());
            s.setZ(low_bits.isZero() && high_bits.isZero());
            s.setC(!high_bits.isZero());
        }
    };

    /**
     * 16-bit (X) times 16-bit (operand) unsigned integer multiply, 32-bit result
     * high word will be in H register, low word in X
     * Z is true if full product is zero
     * N is true if full product is negative (as a signed 32-bit number)
     * C is true if full product does not fit in 16 bits (that is, if H is not zero)
     * V is not affected
     */
    public static final OpCore UMULX = new Dep10OpCore("UMULX", Pep10Table.All) {
        public void exec(Dep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);
            
            var x = s.getX().value();
            var op = operand.value();

            var product = x * op;
            var low_bits = Word.of(product);
            var high_bits = Word.of(product >> 16);

            s.setX(low_bits);
            s.setH(high_bits);

            s.setN(high_bits.isNegative());
            s.setZ(low_bits.isZero() && high_bits.isZero());
            s.setC(!high_bits.isZero());
        }
    };
    
    // TODO UMULSA (unsigned operand, signed A)? SMULUA (signed operand, unsigned A)?

    /**
     * 32-bit (H:A) by 16-bit (operand) signed integer division, 16-bit result
     * quotient will be in A register, remainder in H
     * Z is true if quotient is zero
     * N is true if quotient is negative
     * V is true if quotient does not fit in 16 bits, or divide-by-zero
     * C is true if divide-by-zero
     * On divide-by-zero, quotient and remainder (A and H) will both be zero
     */
    public static final OpCore SDMA = new Dep10OpCore("SDMA", Pep10Table.All) {
        public void exec(Dep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);
            
            var dividend = (s.getH().value() << 16) + s.getA().value();
            var op = operand.signedValue();
            
            if (op == 0) {
                s.setA(Word.of(0));
                s.setH(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setV(true);
                s.setC(true);
            } else {
                var quotient = dividend / op;
                var remainder = dividend % op;
                
                var q = Word.of(quotient);
                s.setA(q);
                s.setH(Word.of(remainder));
                s.setN(q.isNegative());
                s.setZ(q.isZero());
                s.setV(q.signedValue() != quotient);
                s.setC(false);
            }
        }
    };

    /**
     * 32-bit (H:X) by 16-bit (operand) signed integer division, 16-bit result
     * quotient will be in X register, remainder in H
     * Z is true if quotient is zero
     * N is true if quotient is negative
     * V is true if quotient does not fit in 16 bits, or divide-by-zero
     * C is true if divide-by-zero
     * On divide-by-zero, quotient and remainder (X and H) will both be zero
     */
    public static final OpCore SDMX = new Dep10OpCore("SDMX", Pep10Table.All) {
        public void exec(Dep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);
            
            var dividend = (s.getH().value() << 16) + s.getX().value();
            var op = operand.signedValue();
            
            if (op == 0) {
                s.setX(Word.of(0));
                s.setH(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setV(true);
                s.setC(true);
            } else {
                var quotient = dividend / op;
                var remainder = dividend % op;
                
                var q = Word.of(quotient);
                s.setX(q);
                s.setH(Word.of(remainder));
                s.setN(q.isNegative());
                s.setZ(q.isZero());
                s.setV(q.signedValue() != quotient);
                s.setC(false);
            }
        }
    };

    /**
     * 32-bit (H:A) by 16-bit (operand) unsigned integer division, 16-bit result
     * quotient will be in A register, remainder in H
     * Z is true if quotient is zero
     * N is true if quotient is negative (as a signed 16-bit number)
     * V is true if quotient does not fit in 16 bits, or divide-by-zero
     * C is true if divide-by-zero
     * On divide-by-zero, quotient and remainder (A and H) will both be zero
     */
    public static final OpCore UDMA = new Dep10OpCore("UDMA", Pep10Table.All) {
        public void exec(Dep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);
            
            var dividend = ((long) s.getH().value() << 16) + s.getA().value();
            var op = operand.value();
            
            if (op == 0) {
                s.setA(Word.of(0));
                s.setH(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setV(true);
                s.setC(true);
            } else {
                var quotient = dividend / op;
                var remainder = dividend % op;
                
                var q = Word.of((int) quotient);
                s.setA(q);
                s.setH(Word.of((int) remainder));
                s.setN(q.isNegative());
                s.setZ(q.isZero());
                s.setV(q.value() != quotient);
                s.setC(false);
            }
        }
    };

    /**
     * 32-bit (H:X) by 16-bit (operand) unsigned integer division, 16-bit result
     * quotient will be in X register, remainder in H
     * Z is true if quotient is zero
     * N is true if quotient is negative (as a signed 16-bit number)
     * V is true if quotient does not fit in 16 bits, or divide-by-zero
     * C is true if divide-by-zero
     * On divide-by-zero, quotient and remainder (X and H) will both be zero
     */
    public static final OpCore UDMX = new Dep10OpCore("UDMX", Pep10Table.All) {
        public void exec(Dep10Registers s, Mode mode) throws Fault {
            var operand = mode.resolveWord(s);
            
            var dividend = ((long) s.getH().value() << 16) + s.getX().value();
            var op = operand.value();
            
            if (op == 0) {
                s.setX(Word.of(0));
                s.setH(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setV(true);
                s.setC(true);
            } else {
                var quotient = dividend / op;
                var remainder = dividend % op;
                
                var q = Word.of((int) quotient);
                s.setX(q);
                s.setH(Word.of((int) remainder));
                s.setN(q.isNegative());
                s.setZ(q.isZero());
                s.setV(q.value() != quotient);
                s.setC(false);
            }
        }
    };

    private static void setNZ(Pep10Registers s, Word w) {
        s.setN(w.isNegative());
        s.setZ(w.isZero());
    }

    private static Word signExtendByte(Word w) {
        return Word.of((byte) w.value());
    }

    /** Test A: same flags as CPWA 0,i. N and Z from A, V cleared, C set (no borrow). */
    public static final Operation TSTA = new Unary("TSTA") {
        public void exec(Pep10Registers s) {
            setNZ(s, s.getA());
            s.setV(false);
            s.setC(true);
        }
    };

    /** Test X: same flags as CPWX 0,i. */
    public static final Operation TSTX = new Unary("TSTX") {
        public void exec(Pep10Registers s) {
            setNZ(s, s.getX());
            s.setV(false);
            s.setC(true);
        }
    };

    /** Sign-extend the low byte of A into its high byte. N and Z from the result; V and C unchanged. */
    public static final Operation SXBA = new Unary("SXBA") {
        public void exec(Pep10Registers s) {
            var a = signExtendByte(s.getA());
            s.setA(a);
            setNZ(s, a);
        }
    };

    public static final Operation SXBX = new Unary("SXBX") {
        public void exec(Pep10Registers s) {
            var x = signExtendByte(s.getX());
            s.setX(x);
            setNZ(s, x);
        }
    };

    /**
     * Sign-extend A into H, so that H:A is the 32-bit signed value of A.
     * N is the sign of the 32-bit value and Z is true when it is zero; V and C unchanged.
     */
    public static final Operation SXWA = new Dep10Operation.Unary("SXWA") {
        public void exec(Dep10Registers s) {
            var a = s.getA();
            s.setH(Word.of(a.isNegative() ? 0xFFFF : 0));
            setNZ(s, a);
        }
    };

    /** Sign-extend X into H, so that H:X is the 32-bit signed value of X. */
    public static final Operation SXWX = new Dep10Operation.Unary("SXWX") {
        public void exec(Dep10Registers s) {
            var x = s.getX();
            s.setH(Word.of(x.isNegative() ? 0xFFFF : 0));
            setNZ(s, x);
        }
    };

    private static void add(Pep10Registers s, Word r1, Word operand, boolean carryIn, boolean toA) {
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
    private static void sub(Pep10Registers s, Word r1, Word operand, boolean carryIn, boolean toA) {
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
    public static final OpCore ADCA = new OpCore("ADCA", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            add(s, s.getA(), mode.resolveWord(s), s.getC(), true);
        }
    };

    public static final OpCore ADCX = new OpCore("ADCX", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            add(s, s.getX(), mode.resolveWord(s), s.getC(), false);
        }
    };

    /** A <- A - Operand - (1 - C). As with SUBA, C set means no borrow. */
    public static final OpCore SBCA = new OpCore("SBCA", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            sub(s, s.getA(), mode.resolveWord(s), s.getC(), true);
        }
    };

    public static final OpCore SBCX = new OpCore("SBCX", Pep10Table.All) {
        public void exec(Pep10Registers s, Mode mode) throws Fault {
            sub(s, s.getX(), mode.resolveWord(s), s.getC(), false);
        }
    };

    public static Table arithTable() {
        var table = new Table(Pep10Table.UNIMPLEMENTED);

        // unary: A form at 2k, X form at 2k + 1
        Pep10Table.installUnary(table, 2, TSTA);
        Pep10Table.installUnary(table, 3, TSTX);
        Pep10Table.installUnary(table, 4, SXBA);
        Pep10Table.installUnary(table, 5, SXBX);
        Pep10Table.installUnary(table, 6, SXWA);
        Pep10Table.installUnary(table, 7, SXWX);

        // A form at 16k, X form at 16k + 8
        var ops = new OpCore[] { MULA, MULX, DIVA, DIVX, MODA, MODX, UDIVA, UDIVX, UMODA, UMODX, SMULA, SMULX,
                UMULA, UMULX, SDMA, SDMX, UDMA, UDMX, ADCA, ADCX, SBCA, SBCX };
        for (int i = 0; i < ops.length; i++) {
            Pep10Table.installNonUnary(table, 16 + 8 * i, ops[i]);
        }

        return table;
    }
}
