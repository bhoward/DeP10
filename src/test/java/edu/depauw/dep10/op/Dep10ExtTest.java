package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for the extension table TSTr, SXBr, SXWr, ADCr, SBCr (arithmetic table, prefix 0000 1000) and LEAr, SWAPr (utility table, prefix 0000 1001). */
public class Dep10ExtTest {

    private static final int A_SENTINEL = 0xBEEF;
    private static final int X_SENTINEL = 0xDEAD;

    private static State fresh() {
        State s = new State();
        s.setA(Word.of(A_SENTINEL));
        s.setX(Word.of(X_SENTINEL));
        s.setH(Word.of(0x1234));
        return s;
    }

    @Test
    @DisplayName("TSTA/TSTX set the same flags as CPWr 0,i and change nothing else")
    void tstMatchesCompareWithZero() {
        int[] vals = { 0, 1, 0x7FFF, 0x8000, 0xFFFF, 0x1234 };
        for (int v : vals) {
            State t = fresh();
            t.setA(Word.of(v));
            t.setX(Word.of(v));
            State c = fresh();
            c.setA(Word.of(v));
            c.setX(Word.of(v));
            c.setOperand(Word.of(0));

            Dep10Arith.TSTA.exec(t);
            Pep10.CPWA.exec(c, Mode.I);
            assertEquals(c.getN(), t.getN(), "TSTA N for " + v);
            assertEquals(c.getZ(), t.getZ(), "TSTA Z for " + v);
            assertEquals(c.getV(), t.getV(), "TSTA V for " + v);
            assertEquals(c.getC(), t.getC(), "TSTA C for " + v);
            assertEquals(v, t.getA().value(), "TSTA must not change A");

            Dep10Arith.TSTX.exec(t);
            Pep10.CPWX.exec(c, Mode.I);
            assertEquals(c.getN(), t.getN(), "TSTX N for " + v);
            assertEquals(c.getZ(), t.getZ(), "TSTX Z for " + v);
            assertEquals(c.getV(), t.getV(), "TSTX V for " + v);
            assertEquals(c.getC(), t.getC(), "TSTX C for " + v);
            assertEquals(v, t.getX().value(), "TSTX must not change X");
        }
    }

    @Test
    @DisplayName("SXBA sign-extends the low byte of A, X untouched")
    void sxba() {
        State s = fresh();
        s.setA(Word.of(0x1280));
        Dep10Arith.SXBA.exec(s);
        assertEquals(0xFF80, s.getA().value());
        assertTrue(s.getN());
        assertFalse(s.getZ());
        assertEquals(X_SENTINEL, s.getX().value());

        s.setA(Word.of(0xFF7F));
        Dep10Arith.SXBA.exec(s);
        assertEquals(0x007F, s.getA().value());
        assertFalse(s.getN());

        s.setA(Word.of(0xAB00));
        Dep10Arith.SXBA.exec(s);
        assertEquals(0, s.getA().value());
        assertTrue(s.getZ());
    }

    @Test
    @DisplayName("SXBX sign-extends the low byte of X, A untouched")
    void sxbx() {
        State s = fresh();
        s.setX(Word.of(0x00FF));
        Dep10Arith.SXBX.exec(s);
        assertEquals(0xFFFF, s.getX().value());
        assertTrue(s.getN());
        assertEquals(A_SENTINEL, s.getA().value());
    }

    @Test
    @DisplayName("SXWA / SXWX put the sign extension in H and leave the register alone")
    void sxw() {
        State s = fresh();
        s.setA(Word.of(0x8000));
        Dep10Arith.SXWA.exec(s);
        assertEquals(0xFFFF, s.getH().value());
        assertEquals(0x8000, s.getA().value());
        assertTrue(s.getN());

        s.setA(Word.of(0x7FFF));
        Dep10Arith.SXWA.exec(s);
        assertEquals(0, s.getH().value());
        assertFalse(s.getN());

        s.setX(Word.of(0xFFFE));
        Dep10Arith.SXWX.exec(s);
        assertEquals(0xFFFF, s.getH().value());
        assertEquals(0xFFFE, s.getX().value());
        assertEquals(0x7FFF, s.getA().value(), "SXWX must not touch A");

        s.setX(Word.of(0));
        Dep10Arith.SXWX.exec(s);
        assertEquals(0, s.getH().value());
        assertTrue(s.getZ());
    }

    @Test
    @DisplayName("ADCA with C clear is ADDA, with C set adds one more")
    void adcMatchesAddWhenCarryClear() {
        Random rnd = new Random(10);
        for (int i = 0; i < 2000; i++) {
            int a = rnd.nextInt(0x10000);
            int o = rnd.nextInt(0x10000);
            for (boolean c : new boolean[] { false, true }) {
                State s = fresh();
                s.setA(Word.of(a));
                s.setOperand(Word.of(o));
                s.setC(c);
                Dep10Arith.ADCA.exec(s, Mode.I);

                int expected = a + o + (c ? 1 : 0);
                assertEquals(expected & 0xFFFF, s.getA().value(), "ADCA " + a + "+" + o + "+" + c);
                assertEquals(expected > 0xFFFF, s.getC(), "ADCA carry " + a + "+" + o + "+" + c);
                int sa = (short) a, so = (short) o;
                int sr = sa + so + (c ? 1 : 0);
                assertEquals(sr != (short) sr, s.getV(), "ADCA overflow " + a + "+" + o + "+" + c);
                assertEquals((expected & 0xFFFF) == 0, s.getZ());
                assertEquals((expected & 0x8000) != 0, s.getN());
                assertEquals(X_SENTINEL, s.getX().value());

                if (!c) {
                    State t = fresh();
                    t.setA(Word.of(a));
                    t.setOperand(Word.of(o));
                    Pep10.ADDA.exec(t, Mode.I);
                    assertEquals(t.getA(), s.getA());
                    assertEquals(t.getV(), s.getV());
                    assertEquals(t.getC(), s.getC());
                }
            }
        }
    }

    @Test
    @DisplayName("ADCX adds to X and leaves A alone")
    void adcx() {
        State s = fresh();
        s.setX(Word.of(0xFFFF));
        s.setOperand(Word.of(0));
        s.setC(true);
        Dep10Arith.ADCX.exec(s, Mode.I);
        assertEquals(0, s.getX().value());
        assertTrue(s.getC());
        assertTrue(s.getZ());
        assertEquals(A_SENTINEL, s.getA().value());
    }

    @Test
    @DisplayName("SBCA with C set is SUBA, with C clear borrows one more")
    void sbcMatchesSubWhenCarrySet() {
        Random rnd = new Random(11);
        for (int i = 0; i < 2000; i++) {
            int a = rnd.nextInt(0x10000);
            int o = rnd.nextInt(0x10000);
            for (boolean c : new boolean[] { false, true }) {
                State s = fresh();
                s.setA(Word.of(a));
                s.setOperand(Word.of(o));
                s.setC(c);
                Dep10Arith.SBCA.exec(s, Mode.I);

                int borrow = c ? 0 : 1;
                int expected = a - o - borrow;
                assertEquals(expected & 0xFFFF, s.getA().value(), "SBCA " + a + "-" + o + " c=" + c);
                assertEquals(expected >= 0, s.getC(), "SBCA no-borrow carry " + a + "-" + o + " c=" + c);
                int sr = (short) a - (short) o - borrow;
                assertEquals(sr != (short) sr, s.getV(), "SBCA overflow " + a + "-" + o + " c=" + c);
                assertEquals((expected & 0xFFFF) == 0, s.getZ());
                assertEquals((expected & 0x8000) != 0, s.getN());

                if (c) {
                    State t = fresh();
                    t.setA(Word.of(a));
                    t.setOperand(Word.of(o));
                    Pep10.SUBA.exec(t, Mode.I);
                    assertEquals(t.getA(), s.getA());
                    assertEquals(t.getV(), s.getV());
                    assertEquals(t.getC(), s.getC());
                }
            }
        }
    }

    @Test
    @DisplayName("SBCX subtracts from X and leaves A alone")
    void sbcx() {
        State s = fresh();
        s.setX(Word.of(5));
        s.setOperand(Word.of(5));
        s.setC(false);
        Dep10Arith.SBCX.exec(s, Mode.I);
        assertEquals(0xFFFF, s.getX().value());
        assertFalse(s.getC(), "borrow out");
        assertEquals(A_SENTINEL, s.getA().value());
    }

    @Test
    @DisplayName("LEAA / LEAX load the effective address without reading memory")
    void lea() {
        State s = fresh();
        s.setSP(Word.of(0x1000));
        s.setMem2(Word.of(0x1004), Word.of(0x7777));
        s.setOperand(Word.of(4));
        Dep10Util.LEAA.exec(s, Mode.S);
        assertEquals(0x1004, s.getA().value());
        assertEquals(0x7777, s.mem2(Word.of(0x1004)).value(), "memory unchanged");
        assertFalse(s.getZ());

        s.setOperand(Word.of(0x2000));
        Dep10Util.LEAX.exec(s, Mode.D);
        assertEquals(0x2000, s.getX().value());

        s.setX(Word.of(6));
        s.setOperand(Word.of(0x3000));
        Dep10Util.LEAA.exec(s, Mode.X);
        assertEquals(0x3006, s.getA().value());

        s.setOperand(Word.of(0));
        Dep10Util.LEAA.exec(s, Mode.D);
        assertTrue(s.getZ());
    }

    @Test
    @DisplayName("LEAA is not available in immediate mode")
    void leaHasNoImmediateMode() {
        assertEquals(-1, Dep10Util.table.lookup("LEAA", "i"));
        assertEquals(-1, Dep10Util.table.lookup("SWAPA", "i"));
        assertTrue(Dep10Util.table.lookup("LEAA", "s") > 0);
    }

    @Test
    @DisplayName("SWAPA / SWAPX exchange a register with memory")
    void swap() {
        State s = fresh();
        s.setMem2(Word.of(0x2000), Word.of(0x0042));
        s.setOperand(Word.of(0x2000));
        Dep10Util.SWAPA.exec(s, Mode.D);
        assertEquals(0x0042, s.getA().value());
        assertEquals(A_SENTINEL, s.mem2(Word.of(0x2000)).value());
        assertEquals(X_SENTINEL, s.getX().value());

        s.setMem2(Word.of(0x2002), Word.of(0));
        s.setOperand(Word.of(0x2002));
        Dep10Util.SWAPX.exec(s, Mode.D);
        assertEquals(0, s.getX().value());
        assertEquals(X_SENTINEL, s.mem2(Word.of(0x2002)).value());
        assertTrue(s.getZ());
        assertEquals(0x0042, s.getA().value(), "SWAPX must not touch A");
    }

    @Test
    @DisplayName("Opcodes are reachable through Pep10.table: arithmetic table is prefix 0x08, utility table prefix 0x09")
    void tableWiring() {
        assertEquals((1 << 8) | 8, Pep10.table.lookup("TSTA", ""));
        assertEquals((160 << 8) | 8, Pep10.table.lookup("ADCA", "i"));
        assertEquals(((40 + 3) << 8) | 9, Pep10.table.lookup("SWAPX", "s"));
        assertSame(Dep10Arith.SXWX, Pep10.table.get((6 << 8) | 8));
    }

    @Test
    @DisplayName("Two-register arithmetic opcodes are laid out as 000nrxxx: A form at 16k, X form at 16k + 8")
    void arithmeticLayout() {
        String[] families = { "MUL", "DIV", "MOD", "UDIV", "UMOD", "SMUL", "UMUL", "SDM", "UDM", "ADC", "SBC" };
        for (int k = 0; k < families.length; k++) {
            int a = Pep10.table.lookup(families[k] + "A", "i") >> 8;
            int x = Pep10.table.lookup(families[k] + "X", "i") >> 8;
            assertEquals(16 * (k + 1), a, families[k] + "A");
            assertEquals(16 * (k + 1) + 8, x, families[k] + "X");
        }
        // MULr is 0001rxxx
        assertEquals(1, (Pep10.table.lookup("MULA", "i") >> 8) >> 4);
        assertEquals(1, (Pep10.table.lookup("MULX", "i") >> 8) >> 4);
        // unary instructions all sit between 1 and 15
        for (String m : new String[] { "TSTA", "TSTX", "SXBA", "SXBX", "SXWA", "SXWX" }) {
            int code = Pep10.table.lookup(m, "") >> 8;
            assertTrue(code >= 1 && code <= 15, m);
        }
    }
}
