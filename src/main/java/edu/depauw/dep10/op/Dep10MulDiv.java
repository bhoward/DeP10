package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

public class Dep10MulDiv {
    public static final Table table = new Table();

    public static final OpCore MULA = new OpCore("MULA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var a = s.getA().isNegative() ? s.getA().value() - 0x10000 : s.getA().value();

            var op = operand.isNegative() ? operand.value() - 0x10000 : operand.value();

            var product = a * op;
            var low_bits = Word.of(product);

            s.setA(low_bits);

            // N: set if product is <0, cleared otherwise
            s.setN(low_bits.isNegative());

            // Z: set if product is 0, cleared otherwise
            s.setZ(low_bits.isZero());

            // V: overflow value needs to be cleared, so don't set at all (?)
            s.setV(false);

            // C: Carry will only be set if result is less than -2^15 or greater than 2^15 -
            // 1
            s.setC(low_bits.value() != product);
        }
    };

    public static final OpCore MULX = new OpCore("MULX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var x = s.getX().isNegative() ? s.getX().value() - 0x10000 : s.getX().value();

            var op = operand.isNegative() ? operand.value() - 0x10000 : operand.value();

            var product = x * op;
            var low_bits = Word.of(product);

            s.setX(low_bits);

            // N: set if product is <0, cleared otherwise
            s.setN(low_bits.isNegative());

            // Z: set if product is 0, cleared otherwise
            s.setZ(low_bits.isZero());

            // V: overflow value needs to be cleared, so don't set at all (?)
            s.setV(false);

            // C: Carry will only be set if result is less than -2^15 or greater than 2^15 -
            // 1
            s.setC(low_bits.value() != product);
        }
    };

    public static final OpCore MULHA = new OpCore("MULHA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            // update logic for signed * signed (0x10000 - 65536 states for two's
            // complement)
            var a = s.getA().isNegative() ? s.getA().value() - 0x10000 : s.getA().value();
            var op = operand.isNegative() ? operand.value() - 0x10000 : operand.value();

            var product = a * op;
            var high_bits = Word.of(product >>> 16);

            s.setA(high_bits);

            s.setN(high_bits.isNegative());
            s.setZ(high_bits.isZero());
            // Confirmed with Brian [9/4/2026]: V/C always false for MULH*, no meaningful
            // signal (matches ARM long-multiply convention)
            s.setV(false);
            s.setC(false);
        }
    };

    public static final OpCore MULHX = new OpCore("MULHX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            // update logic for signed * signed (0x10000 - 65536 states for two's
            // complement)
            var x = s.getX().isNegative() ? s.getX().value() - 0x10000 : s.getX().value();
            var op = operand.isNegative() ? operand.value() - 0x10000 : operand.value();

            var product = x * op;
            var high_bits = Word.of(product >>> 16);

            s.setX(high_bits);

            s.setN(high_bits.isNegative());
            s.setZ(high_bits.isZero());
            // Confirmed with Brian [9/4/2026]: V/C always false for MULH*, no meaningful
            // signal (matches ARM long-multiply convention)
            s.setV(false);
            s.setC(false);
        }
    };

    public static final OpCore UMULHA = new OpCore("UMULHA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var a = s.getA().value();
            var op = operand.value();

            var product = a * op;
            var high_bits = Word.of(product >> 16);

            s.setA(high_bits);

            s.setN(high_bits.isNegative());
            // Confirmed with Brian: N reflects high-bits sign regardless of signed/unsigned
            s.setZ(high_bits.isZero());
            // Confirmed with Brian [9/4/2026]: V/C always false for MULH*, no meaningful
            // signal (matches ARM long-multiply convention)
            s.setV(false);
            s.setC(false);
        }
    };

    public static final OpCore UMULHX = new OpCore("UMULHX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var x = s.getX().value();
            var op = operand.value();

            var product = x * op;
            var high_bits = Word.of(product >> 16);

            s.setX(high_bits);

            s.setN(high_bits.isNegative());
            // Confirmed with Brian: N reflects high-bits sign regardless of signed/unsigned
            s.setZ(high_bits.isZero());
            // Confirmed with Brian [9/4/2026]: V/C always false for MULH*, no meaningful
            // signal (matches ARM long-multiply convention)
            s.setV(false);
            s.setC(false);
        }
    };

    public static final OpCore DIVA = new OpCore("DIVA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            // update logic for signed / signed (0x10000 - 65536 states for two's
            // complement)
            var a = s.getA().isNegative() ? s.getA().value() - 0x10000 : s.getA().value();
            var op = operand.isNegative() ? operand.value() - 0x10000 : operand.value();

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

            // N: set if quotient is <0, cleared otherwise
            s.setN(quotient < 0);

            // Z: set if quotient is 0, cleared otherwise
            s.setZ(quotient == 0);

            // V: overflow only when -32768 / -1 (== -32768)
            s.setV(a == Short.MIN_VALUE && op == -1);

            // C: Carry false unless divide by zero
            s.setC(false);
        }
    };

    public static final OpCore DIVX = new OpCore("DIVX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            // update logic for signed / signed (0x10000 - 65536 states for two's
            // complement)
            var x = s.getX().isNegative() ? s.getX().value() - 0x10000 : s.getX().value();
            var op = operand.isNegative() ? operand.value() - 0x10000 : operand.value();

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

            // N: set if quotient is <0, cleared otherwise
            s.setN(quotient < 0);

            // Z: set if quotient is 0, cleared otherwise
            s.setZ(quotient == 0);

            // V: overflow only when -32768 / -1 (== -32768)
            s.setV(x == Short.MIN_VALUE && op == -1);

            // C: Carry false unless divide by zero
            s.setC(false);
        }
    };

    public static final OpCore MODA = new OpCore("MODA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            // update logic for signed % signed (0x10000 - 65536 states for two's
            // complement)
            var a = s.getA().isNegative() ? s.getA().value() - 0x10000 : s.getA().value();
            var op = operand.isNegative() ? operand.value() - 0x10000 : operand.value();

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

            // N: set if remainder is <0, cleared otherwise
            s.setN(remainder < 0);

            // Z: set if remainder is 0, cleared otherwise
            s.setZ(remainder == 0);

            // V: overflow value needs to be cleared
            s.setV(false);

            // C: Carry false unless divide by zero
            s.setC(false);
        }
    };

    public static final OpCore MODX = new OpCore("MODX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            // update logic for signed % signed (0x10000 - 65536 states for two's
            // complement)
            var x = s.getX().isNegative() ? s.getX().value() - 0x10000 : s.getX().value();
            var op = operand.isNegative() ? operand.value() - 0x10000 : operand.value();

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

            // N: set if remainder is <0, cleared otherwise
            s.setN(remainder < 0);

            // Z: set if remainder is 0, cleared otherwise
            s.setZ(remainder == 0);

            // V: overflow value needs to be cleared
            s.setV(false);

            // C: Carry false unless divide by zero
            s.setC(false);
        }
    };

    public static final OpCore UDIVA = new OpCore("UDIVA", Modes.All) {
        public void exec(State s, Mode mode) {
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

            s.setA(Word.of(quotient));

            // N: set if quotient is <0, cleared otherwise (???)
            s.setN(false);

            // Z: set if quotient is 0, cleared otherwise
            s.setZ(quotient == 0);

            // V: no overflow
            s.setV(false);

            // C: Carry false unless divide by zero
            s.setC(false);
        }
    };

    public static final OpCore UDIVX = new OpCore("UDIVX", Modes.All) {
        public void exec(State s, Mode mode) {
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

            s.setX(Word.of(quotient));

            // N: set if quotient is <0, cleared otherwise (???)
            s.setN(false);

            // Z: set if quotient is 0, cleared otherwise
            s.setZ(quotient == 0);

            // V: no overflow
            s.setV(false);

            // C: Carry false unless divide by zero
            s.setC(false);
        }
    };

    public static final OpCore UMODA = new OpCore("UMODA", Modes.All) {
        public void exec(State s, Mode mode) {
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

            s.setA(Word.of(remainder));

            // N: set if remainder is <0, cleared otherwise (???)
            s.setN(false);

            // Z: set if remainder is 0, cleared otherwise
            s.setZ(remainder == 0);

            // V: overflow value needs to be cleared
            s.setV(false);

            // C: Carry false unless divide by zero
            s.setC(false);
        }
    };

    public static final OpCore UMODX = new OpCore("UMODX", Modes.All) {
        public void exec(State s, Mode mode) {
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

            s.setX(Word.of(remainder));

            // N: set if remainder is <0, cleared otherwise (???)
            s.setN(false);

            // Z: set if remainder is 0, cleared otherwise
            s.setZ(remainder == 0);

            // V: overflow value needs to be cleared
            s.setV(false);

            // C: Carry false unless divide by zero
            s.setC(false);
        }
    };

    static {
        table.install(8, MULA); // NOTE opcode 0 should be unimplemented in any table, except perhaps as a
        // prefix
        table.install(16, MULX);
        table.install(24, MULHA);
        table.install(32, MULHX);
        table.install(40, UMULHA);
        table.install(48, UMULHX);
        table.install(56, DIVA);
        table.install(64, DIVX);
        table.install(72, MODA);
        table.install(80, MODX);
        table.install(88, UDIVA);
        table.install(96, UDIVX);
        table.install(104, UMODA);
        table.install(112, UMODX);

    }
}
