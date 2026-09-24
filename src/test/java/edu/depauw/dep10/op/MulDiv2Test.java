package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Dep10MulDiv2 (SMULA/SMULX/UMULA/UMULX/SDIVA/SDIVX/UDIVA/UDIVX), the
 * H-register-based instruction set that opcode 8 has routed to since the h_register
 * merge (0.9.0). Dep10MulDiv (the old MULA/DIVA/MODA/... set tested in MulDivTest)
 * is no longer reachable from any opcode.
 *
 * Several tests below are regressions for Issue #10: three register mix-ups found
 * by code review before this table was merged (UMULX writing into A instead of X;
 * SDIVX's divide-by-zero branch zeroing A instead of X; UDIVX being a full copy of
 * UDIVA that never touched X). Brian's fix (commit ff593c1) addressed all three; these
 * tests pin that behavior down so it can't silently regress. Every A/X pair below is
 * exercised with A and X holding different sentinel values from the start, per the
 * testing lesson noted in the DDIV design proposal after Issue #10.
 */
public class MulDiv2Test {

    private static final int A_SENTINEL = 0xBEEF;
    private static final int X_SENTINEL = 0xDEAD;

    private static State freshA(int a, int h, int operand) {
        State s = new State();
        s.setA(Word.of(a));
        s.setX(Word.of(X_SENTINEL));
        s.setH(Word.of(h));
        s.setOperand(Word.of(operand));
        return s;
    }

    private static State freshX(int x, int h, int operand) {
        State s = new State();
        s.setX(Word.of(x));
        s.setA(Word.of(A_SENTINEL));
        s.setH(Word.of(h));
        s.setOperand(Word.of(operand));
        return s;
    }

    @Nested
    @DisplayName("SMULA / SMULX (signed 16x16->32 multiply)")
    class SignedMultiply {

        @Test
        @DisplayName("SMULA: 3 * 4 = 12, fits in low word, H=0, no flags")
        void smula_positive() {
            State s = freshA(3, 0, 4);
            Dep10MulDiv2.SMULA.exec(s, Mode.I);

            assertEquals(12, s.getA().value());
            assertEquals(0, s.getH().value());
            assertEquals(X_SENTINEL, s.getX().value(), "SMULA must not touch X");
            assertFalse(s.getN());
            assertFalse(s.getZ());
            assertFalse(s.getC());
        }

        @Test
        @DisplayName("SMULA: -3 * 4 = -12, H is sign-extension of A, N set, C clear")
        void smula_negative() {
            State s = freshA(Word.of(-3).value(), 0, 4);
            Dep10MulDiv2.SMULA.exec(s, Mode.I);

            assertEquals(-12, s.getA().signedValue());
            assertEquals(0xFFFF, s.getH().value(), "H should be all-ones sign-extension");
            assertTrue(s.getN());
            assertFalse(s.getC(), "C should be clear because H is just the sign-extend of A");
        }

        @Test
        @DisplayName("SMULA: 20000 * 20000 overflows 16 bits, C set, H holds real high word")
        void smula_overflowsSetsCarry() {
            State s = freshA(20000, 0, 20000);
            Dep10MulDiv2.SMULA.exec(s, Mode.I);

            long product = 400_000_000L;
            assertEquals((int) (product & 0xFFFF), s.getA().value());
            assertEquals((int) ((product >>> 16) & 0xFFFF), s.getH().value());
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("SMULX: -3 * 4 = -12 written to X and H, A untouched")
        void smulx_writesToX() {
            State s = freshX(Word.of(-3).value(), 0, 4);
            Dep10MulDiv2.SMULX.exec(s, Mode.I);

            assertEquals(-12, s.getX().signedValue());
            assertEquals(0xFFFF, s.getH().value());
            assertEquals(A_SENTINEL, s.getA().value(), "SMULX must not touch A");
        }

        @Test
        @DisplayName("SMULA: 0 * anything = 0, Z set")
        void smula_zero() {
            State s = freshA(0, 0, 12345);
            Dep10MulDiv2.SMULA.exec(s, Mode.I);
            assertEquals(0, s.getA().value());
            assertEquals(0, s.getH().value());
            assertTrue(s.getZ());
        }
    }

    @Nested
    @DisplayName("UMULA / UMULX (unsigned 16x16->32 multiply)")
    class UnsignedMultiply {

        @Test
        @DisplayName("UMULA: 3 * 4 = 12, H=0, C clear")
        void umula_positive() {
            State s = freshA(3, 0, 4);
            Dep10MulDiv2.UMULA.exec(s, Mode.I);
            assertEquals(12, s.getA().value());
            assertEquals(0, s.getH().value());
            assertFalse(s.getC());
        }

        @Test
        @DisplayName("UMULA: 60000 * 60000 exceeds 16 bits, C set, H nonzero")
        void umula_overflow() {
            State s = freshA(60000, 0, 60000);
            Dep10MulDiv2.UMULA.exec(s, Mode.I);
            long product = 60000L * 60000L;
            assertEquals((int) (product & 0xFFFF), s.getA().value());
            assertEquals((int) ((product >>> 16) & 0xFFFF), s.getH().value());
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("REGRESSION (Issue #10): UMULX writes its product into X, not A")
        void umulx_regressionWritesToX() {
            State s = freshX(3, 0, 4);
            Dep10MulDiv2.UMULX.exec(s, Mode.I);

            assertEquals(12, s.getX().value(), "UMULX must write the low word into X");
            assertEquals(A_SENTINEL, s.getA().value(), "UMULX must leave A untouched");
        }
    }

    @Nested
    @DisplayName("SDIVA / SDIVX (signed 32-by-16 divide, H:A or H:X)")
    class SignedDivide {

        @Test
        @DisplayName("SDIVA: 100 / 7 = 14 r 2, no flags")
        void sdiva_normal() {
            State s = freshA(100, 0, 7);
            Dep10MulDiv2.SDIVA.exec(s, Mode.I);
            assertEquals(14, s.getA().value());
            assertEquals(2, s.getH().value());
            assertFalse(s.getC());
            assertFalse(s.getV());
        }

        @Test
        @DisplayName("SDIVA: -100 / 7 = -14 r -2 (truncating division), N set")
        void sdiva_negativeDividend() {
            long dividend = -100L;
            int h = (int) ((dividend >> 16) & 0xFFFF);
            int a = (int) (dividend & 0xFFFF);
            State s = freshA(a, h, 7);
            Dep10MulDiv2.SDIVA.exec(s, Mode.I);

            assertEquals(-14, s.getA().signedValue());
            assertEquals(-2, s.getH().signedValue());
            assertTrue(s.getN());
        }

        @Test
        @DisplayName("SDIVA: divide by zero zeroes A and H, N=0 Z=1 V=1 C=1")
        void sdiva_divideByZero() {
            State s = freshA(100, 0, 0);
            Dep10MulDiv2.SDIVA.exec(s, Mode.I);

            assertEquals(0, s.getA().value());
            assertEquals(0, s.getH().value());
            assertFalse(s.getN());
            assertTrue(s.getZ());
            assertTrue(s.getV());
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("REGRESSION (Issue #10): SDIVX divide-by-zero zeroes X, not A")
        void sdivx_regressionDivideByZeroZeroesX() {
            State s = freshX(100, 0, 0);
            Dep10MulDiv2.SDIVX.exec(s, Mode.I);

            assertEquals(0, s.getX().value(), "SDIVX divide-by-zero must zero X");
            assertEquals(A_SENTINEL, s.getA().value(), "SDIVX divide-by-zero must not touch A");
            assertEquals(0, s.getH().value());
            assertTrue(s.getC());
            assertTrue(s.getV());
        }

        @Test
        @DisplayName("SDIVX: 100 / 7 = 14 r 2 in X, A untouched")
        void sdivx_normal() {
            State s = freshX(100, 0, 7);
            Dep10MulDiv2.SDIVX.exec(s, Mode.I);
            assertEquals(14, s.getX().value());
            assertEquals(2, s.getH().value());
            assertEquals(A_SENTINEL, s.getA().value());
        }
    }

    @Nested
    @DisplayName("UDIVA / UDIVX (unsigned 32-by-16 divide, H:A or H:X)")
    class UnsignedDivide {

        @Test
        @DisplayName("UDIVA: 100 / 7 = 14 r 2")
        void udiva_normal() {
            State s = freshA(100, 0, 7);
            Dep10MulDiv2.UDIVA.exec(s, Mode.I);
            assertEquals(14, s.getA().value());
            assertEquals(2, s.getH().value());
        }

        @Test
        @DisplayName("UDIVA: dividend spans H:A (65536 / 100 = 655 r 36)")
        void udiva_spansHighWord() {
            State s = freshA(0, 1, 100);
            Dep10MulDiv2.UDIVA.exec(s, Mode.I);
            assertEquals(655, s.getA().value());
            assertEquals(36, s.getH().value());
        }

        @Test
        @DisplayName("REGRESSION (Issue #10): UDIVX actually divides using X, not a copy of UDIVA")
        void udivx_regressionUsesX() {
            State s = freshX(100, 0, 7);
            Dep10MulDiv2.UDIVX.exec(s, Mode.I);

            assertEquals(14, s.getX().value(), "UDIVX must write the quotient into X");
            assertEquals(2, s.getH().value());
            assertEquals(A_SENTINEL, s.getA().value(), "UDIVX must not touch A");
        }

        @Test
        @DisplayName("REGRESSION (Issue #10): UDIVX divide-by-zero zeroes X, not A")
        void udivx_regressionDivideByZero() {
            State s = freshX(100, 0, 0);
            Dep10MulDiv2.UDIVX.exec(s, Mode.I);

            assertEquals(0, s.getX().value());
            assertEquals(A_SENTINEL, s.getA().value());
            assertTrue(s.getC());
            assertTrue(s.getV());
        }
    }

    @Nested
    @DisplayName("Opcode table wiring")
    class TableWiring {

        @Test
        @DisplayName("Top-level opcode 8 routes to Dep10MulDiv2, not the retired Dep10MulDiv")
        void opcode8RoutesToMulDiv2() {
            var entry = Pep10.table.getOp(edu.depauw.dep10.util.UByte.of(8));
            assertTrue(entry instanceof Operation.Prefix, "opcode 8 should be a prefix into an extension table");
            var prefix = (Operation.Prefix) entry;
            assertSame(Dep10MulDiv2.table, prefix.getTable());
        }
    }
}
