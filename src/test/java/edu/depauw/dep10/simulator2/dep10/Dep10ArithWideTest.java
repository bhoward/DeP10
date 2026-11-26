package edu.depauw.dep10.simulator2.dep10;

import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.Table;
import edu.depauw.dep10.simulator2.pep10.Pep10Image;
import edu.depauw.dep10.simulator2.pep10.Pep10Table;
import edu.depauw.dep10.simulator2.pep10.Mode;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the H-register-based half of Dep10Arith: SMULA/SMULX, UMULA/UMULX,
 * SDMA/SDMX, and UDMA/UDMX. As of 0.9.1, Dep10Arith and the former Dep10Arith2
 * are a single merged table at opcode 8 (see MulDivTest for the plain 16-bit
 * half: MULr/DIVr/UDIVr/MODr/UMODr). SDMA/SDMX/UDMA/UDMX were renamed from
 * SDIVA/SDIVX/UDIVA/UDIVX (9/24/2026 meeting) to reflect that they compute a
 * quotient AND a remainder together (via H), unlike the plain 16-bit DIV/MOD,
 * which are single-result.
 *
 * Several tests below are regressions for Issue #10: three register mix-ups found
 * by code review before this table was merged into main (UMULX writing into A
 * instead of X; SDMX's divide-by-zero branch zeroing A instead of X; UDMX being a
 * full copy of UDMA that never touched X). Brian's fix (commit ff593c1) addressed
 * all three; these tests pin that behavior down so it can't silently regress.
 * Every A/X pair below is exercised with A and X holding different sentinel
 * values from the start, per the testing lesson noted in the DDIV design
 * proposal after Issue #10.
 */
public class Dep10ArithWideTest {

    /** A DeP10 state with zeroed memory and registers. */
    private static Dep10State newState() {
        return new Dep10Machine().init(memory -> {
            for (int i = 0; i < memory.size(); i++) {
                memory.loadB(Word.of(i), UByte.of(0));
            }
        });
    }


    private static final int A_SENTINEL = 0xBEEF;
    private static final int X_SENTINEL = 0xDEAD;

    private static Dep10State freshA(int a, int h, int operand) {
        Dep10State s = newState();
        s.setA(Word.of(a));
        s.setX(Word.of(X_SENTINEL));
        s.setH(Word.of(h));
        s.setOperand(Word.of(operand));
        return s;
    }

    private static Dep10State freshX(int x, int h, int operand) {
        Dep10State s = newState();
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
        void smula_positive() throws Fault {
            Dep10State s = freshA(3, 0, 4);
            Dep10Arith.SMULA.exec(s, Mode.I);

            assertEquals(12, s.getA().value());
            assertEquals(0, s.getH().value());
            assertEquals(X_SENTINEL, s.getX().value(), "SMULA must not touch X");
            assertFalse(s.getN());
            assertFalse(s.getZ());
            assertFalse(s.getC());
        }

        @Test
        @DisplayName("SMULA: -3 * 4 = -12, H is sign-extension of A, N set, C clear")
        void smula_negative() throws Fault {
            Dep10State s = freshA(Word.of(-3).value(), 0, 4);
            Dep10Arith.SMULA.exec(s, Mode.I);

            assertEquals(-12, s.getA().signedValue());
            assertEquals(0xFFFF, s.getH().value(), "H should be all-ones sign-extension");
            assertTrue(s.getN());
            assertFalse(s.getC(), "C should be clear because H is just the sign-extend of A");
        }

        @Test
        @DisplayName("SMULA: 20000 * 20000 overflows 16 bits, C set, H holds real high word")
        void smula_overflowsSetsCarry() throws Fault {
            Dep10State s = freshA(20000, 0, 20000);
            Dep10Arith.SMULA.exec(s, Mode.I);

            long product = 400_000_000L;
            assertEquals((int) (product & 0xFFFF), s.getA().value());
            assertEquals((int) ((product >>> 16) & 0xFFFF), s.getH().value());
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("SMULX: -3 * 4 = -12 written to X and H, A untouched")
        void smulx_writesToX() throws Fault {
            Dep10State s = freshX(Word.of(-3).value(), 0, 4);
            Dep10Arith.SMULX.exec(s, Mode.I);

            assertEquals(-12, s.getX().signedValue());
            assertEquals(0xFFFF, s.getH().value());
            assertEquals(A_SENTINEL, s.getA().value(), "SMULX must not touch A");
        }

        @Test
        @DisplayName("SMULA: 0 * anything = 0, Z set")
        void smula_zero() throws Fault {
            Dep10State s = freshA(0, 0, 12345);
            Dep10Arith.SMULA.exec(s, Mode.I);
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
        void umula_positive() throws Fault {
            Dep10State s = freshA(3, 0, 4);
            Dep10Arith.UMULA.exec(s, Mode.I);
            assertEquals(12, s.getA().value());
            assertEquals(0, s.getH().value());
            assertFalse(s.getC());
        }

        @Test
        @DisplayName("UMULA: 60000 * 60000 exceeds 16 bits, C set, H nonzero")
        void umula_overflow() throws Fault {
            Dep10State s = freshA(60000, 0, 60000);
            Dep10Arith.UMULA.exec(s, Mode.I);
            long product = 60000L * 60000L;
            assertEquals((int) (product & 0xFFFF), s.getA().value());
            assertEquals((int) ((product >>> 16) & 0xFFFF), s.getH().value());
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("REGRESSION (Issue #10): UMULX writes its product into X, not A")
        void umulx_regressionWritesToX() throws Fault {
            Dep10State s = freshX(3, 0, 4);
            Dep10Arith.UMULX.exec(s, Mode.I);

            assertEquals(12, s.getX().value(), "UMULX must write the low word into X");
            assertEquals(A_SENTINEL, s.getA().value(), "UMULX must leave A untouched");
        }
    }

    @Nested
    @DisplayName("SDMA / SDMX (signed 32-by-16 divide+mod, H:A or H:X)")
    class SignedDivideMod {

        @Test
        @DisplayName("SDMA: 100 / 7 = 14 r 2, no flags")
        void sdma_normal() throws Fault {
            Dep10State s = freshA(100, 0, 7);
            Dep10Arith.SDMA.exec(s, Mode.I);
            assertEquals(14, s.getA().value());
            assertEquals(2, s.getH().value());
            assertFalse(s.getC());
            assertFalse(s.getV());
        }

        @Test
        @DisplayName("SDMA: -100 / 7 = -14 r -2 (truncating division), N set")
        void sdma_negativeDividend() throws Fault {
            long dividend = -100L;
            int h = (int) ((dividend >> 16) & 0xFFFF);
            int a = (int) (dividend & 0xFFFF);
            Dep10State s = freshA(a, h, 7);
            Dep10Arith.SDMA.exec(s, Mode.I);

            assertEquals(-14, s.getA().signedValue());
            assertEquals(-2, s.getH().signedValue());
            assertTrue(s.getN());
        }

        @Test
        @DisplayName("SDMA: divide by zero zeroes A and H, N=0 Z=1 V=1 C=1")
        void sdma_divideByZero() throws Fault {
            Dep10State s = freshA(100, 0, 0);
            Dep10Arith.SDMA.exec(s, Mode.I);

            assertEquals(0, s.getA().value());
            assertEquals(0, s.getH().value());
            assertFalse(s.getN());
            assertTrue(s.getZ());
            assertTrue(s.getV());
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("REGRESSION (Issue #10): SDMX divide-by-zero zeroes X, not A")
        void sdmx_regressionDivideByZeroZeroesX() throws Fault {
            Dep10State s = freshX(100, 0, 0);
            Dep10Arith.SDMX.exec(s, Mode.I);

            assertEquals(0, s.getX().value(), "SDMX divide-by-zero must zero X");
            assertEquals(A_SENTINEL, s.getA().value(), "SDMX divide-by-zero must not touch A");
            assertEquals(0, s.getH().value());
            assertTrue(s.getC());
            assertTrue(s.getV());
        }

        @Test
        @DisplayName("SDMX: 100 / 7 = 14 r 2 in X, A untouched")
        void sdmx_normal() throws Fault {
            Dep10State s = freshX(100, 0, 7);
            Dep10Arith.SDMX.exec(s, Mode.I);
            assertEquals(14, s.getX().value());
            assertEquals(2, s.getH().value());
            assertEquals(A_SENTINEL, s.getA().value());
        }
    }

    @Nested
    @DisplayName("UDMA / UDMX (unsigned 32-by-16 divide+mod, H:A or H:X)")
    class UnsignedDivideMod {

        @Test
        @DisplayName("UDMA: 100 / 7 = 14 r 2")
        void udma_normal() throws Fault {
            Dep10State s = freshA(100, 0, 7);
            Dep10Arith.UDMA.exec(s, Mode.I);
            assertEquals(14, s.getA().value());
            assertEquals(2, s.getH().value());
        }

        @Test
        @DisplayName("UDMA: dividend spans H:A (65536 / 100 = 655 r 36)")
        void udma_spansHighWord() throws Fault {
            Dep10State s = freshA(0, 1, 100);
            Dep10Arith.UDMA.exec(s, Mode.I);
            assertEquals(655, s.getA().value());
            assertEquals(36, s.getH().value());
        }

        @Test
        @DisplayName("REGRESSION (Issue #10): UDMX actually divides using X, not a copy of UDMA")
        void udmx_regressionUsesX() throws Fault {
            Dep10State s = freshX(100, 0, 7);
            Dep10Arith.UDMX.exec(s, Mode.I);

            assertEquals(14, s.getX().value(), "UDMX must write the quotient into X");
            assertEquals(2, s.getH().value());
            assertEquals(A_SENTINEL, s.getA().value(), "UDMX must not touch A");
        }

        @Test
        @DisplayName("REGRESSION (Issue #10): UDMX divide-by-zero zeroes X, not A")
        void udmx_regressionDivideByZero() throws Fault {
            Dep10State s = freshX(100, 0, 0);
            Dep10Arith.UDMX.exec(s, Mode.I);

            assertEquals(0, s.getX().value());
            assertEquals(A_SENTINEL, s.getA().value());
            assertTrue(s.getC());
            assertTrue(s.getV());
        }
    }

    @Nested
    @DisplayName("Opcode table wiring")
    class TableWiring {

        private Table table() {
            var table = new ExtensibleTable(Pep10Table.UNIMPLEMENTED);
            Dep10Table.installDep10(table);
            return table;
        }

        @Test
        @DisplayName("Top-level opcode 8 routes to the arithmetic table")
        void opcode8RoutesToArithTable() throws Fault {
            var entry = table().getOp(edu.depauw.dep10.util.UByte.of(8));
            assertTrue(entry instanceof ExtensibleTable.Prefix, "opcode 8 should be a prefix into an extension table");
        }

        @Test
        @DisplayName("Lookup codes: prefix in the high byte, second opcode in the low byte")
        void lookupCodes() {
            var table = table();
            assertEquals(0x0810, table.lookup("MULA", "i"));
            assertEquals(0x0811, table.lookup("MULA", "d"));
            assertEquals(0x0818, table.lookup("MULX", "i"));
            assertEquals(0x0820, table.lookup("DIVA", "i"));
            assertEquals(0x0860, table.lookup("SMULA", "i"));
            assertEquals(0x0898, table.lookup("UDMX", "i"));
            assertEquals(0x08B8, table.lookup("SBCX", "i"));
            assertEquals(0x08B8 + 7, table.lookup("SBCX", "sfx"));
            assertEquals(0x0802, table.lookup("TSTA", ""));
            assertEquals(0x0807, table.lookup("SXWX", ""));
        }

        @Test
        @DisplayName("Every arithmetic instruction is installed in all its addressing modes")
        void allInstalled() {
            var table = table();
            for (var name : new String[] { "MULA", "MULX", "DIVA", "DIVX", "MODA", "MODX", "UDIVA", "UDIVX", "UMODA",
                    "UMODX", "SMULA", "SMULX", "UMULA", "UMULX", "SDMA", "SDMX", "UDMA", "UDMX", "ADCA", "ADCX", "SBCA",
                    "SBCX" }) {
                for (var mode : new String[] { "i", "d", "n", "s", "sf", "x", "sx", "sfx" }) {
                    assertTrue(table.lookup(name, mode) >= 0, name + "," + mode);
                }
            }
        }
    }

    @Nested
    @DisplayName("Running prefixed instructions on the machine")
    class OnTheMachine {
        private Dep10State run(String object, int steps) throws Fault {
            var machine = new Dep10Machine();
            var image = new Pep10Image();
            image.addString(object + " [FFF9] 00 00 FB 8F");
            var state = machine.init(image);
            for (int i = 0; i < steps; i++) {
                machine.fetch(state).execute(state);
            }
            return state;
        }

        @Test
        @DisplayName("LDWA 6,i then SMULA 1000,i leaves the 32-bit product in H:A")
        void smulaOnMachine() throws Fault {
            // C0 0006: LDWA 6,i;  08 60 03E8: SMULA 1000,i
            var s = run("C0 00 06 08 60 03 E8", 2);
            assertEquals(6000, s.getA().value());
            assertEquals(0, s.getH().value());
            assertEquals(7, s.getPC().value());
        }

        @Test
        @DisplayName("SXWA then SDMA divides the 32-bit value in H:A")
        void sxwaThenSdma() throws Fault {
            // LDWA -7,i; SXWA; SDMA 2,i
            var s = run("C0 FF F9 08 06 08 80 00 02", 3);
            assertEquals(0xFFFD, s.getA().value()); // -3
            assertEquals(0xFFFF, s.getH().value()); // remainder -1
        }

        @Test
        @DisplayName("ADCA adds the carry from the previous addition (a 32-bit add)")
        void adcaChain() throws Fault {
            // LDWA FFFF,i; ADDA 1,i (A = 0, C set); ADCA 0,i (A = 1)
            var s = run("C0 FF FF 50 00 01 08 A0 00 00", 3);
            assertEquals(1, s.getA().value());
        }

        @Test
        @DisplayName("TSTA sets N and Z from A and C (no borrow), clears V")
        void tsta() throws Fault {
            var s = run("C0 80 00 08 02", 2); // LDWA 8000,i; TSTA
            assertTrue(s.getN());
            assertFalse(s.getZ());
            assertFalse(s.getV());
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("SXBA sign-extends the low byte of A")
        void sxba() throws Fault {
            var s = run("C0 01 80 08 04", 2); // LDWA 0180,i; SXBA
            assertEquals(0xFF80, s.getA().value());
            assertTrue(s.getN());
        }

        @Test
        @DisplayName("SXWX copies the sign of X into H")
        void sxwx() throws Fault {
            var s = run("C8 80 00 08 07", 2); // LDWX 8000,i; SXWX
            assertEquals(0xFFFF, s.getH().value());
            var t = run("C8 00 05 08 07", 2);
            assertEquals(0, t.getH().value());
        }

        @Test
        @DisplayName("SBCA subtracts with borrow: 32-bit subtraction low then high word")
        void sbca() throws Fault {
            // LDWA 0000,i; SUBA 0001,i (A = FFFF, C clear); SBCA 0000,i (A = FFFE)
            var s = run("C0 00 00 60 00 01 08 B0 00 00", 3);
            assertEquals(0xFFFE, s.getA().value());
        }
    }
}
