package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

public class MulDivTest {

    private static int u16(int signedValue) {
        return signedValue & 0xFFFF;
    }

    private static State freshState(int aValue, int operandValue) {
        State s = new State();
        s.setA(Word.of(aValue));
        s.setX(Word.of(aValue));
        s.setOperand(Word.of(operandValue));
        return s;
    }

    @Nested
    @DisplayName("MULA / MULX (low-word multiply)")
    class MulLow {

        @Test
        @DisplayName("MULA: 5 * 3 = 15, positive result, no flags set")
        void testMula_positiveResult_correctValueAndFlags() {
            State s = freshState(5, 3);
            Dep10MulDiv.MULA.exec(s, Mode.I);

            assertEquals(15, s.getA().value());
            assertFalse(s.getN());
            assertFalse(s.getZ());
            assertFalse(s.getV());
            assertFalse(s.getC());
        }

        @Test
        @DisplayName("BUG: MULA: -5 * 3 = -15, N flag should be set but is not")
        void testMula_negativeResult_nFlagShouldBeSet() {
            State s = freshState(-5, 3);
            Dep10MulDiv.MULA.exec(s, Mode.I);

            assertEquals(u16(-15), s.getA().value(), "low 16 bits of product should be -15");
            assertTrue(s.getN(), "N flag should be set because the result (-15) is negative");
        }

        @Test
        @DisplayName("BUG: MULA: 256 * 256 = 65536, low word is 0, Z flag should be set but is not")
        void testMula_lowWordZeroButFullProductNonzero_zFlagShouldBeSet() {
            State s = freshState(256, 256);
            Dep10MulDiv.MULA.exec(s, Mode.I);

            assertEquals(0, s.getA().value(), "low 16 bits of 65536 should be 0");
            assertTrue(s.getZ(), "Z flag should be set because the stored result is 0");
        }

        @Test
        @DisplayName("MULA: carry set when product exceeds 16 bits")
        void testMula_largeProduct_carrySet() {
            // FIX: the previous version of this test used 300 * 40 = 12000, which
            // does NOT exceed 16 bits, so it was asserting assertFalse(C) under a
            // "carry set" display name -- passing for the wrong reason. 300 * 300
            // = 90000 genuinely overflows a 16-bit product.
            State s = freshState(300, 300);
            Dep10MulDiv.MULA.exec(s, Mode.I);
            assertEquals(u16(90000), s.getA().value(), "low 16 bits of 90000");
            assertTrue(s.getC(), "C should be set because 90000 doesn't fit in 16 bits");
        }

        @Test
        @DisplayName("MULA: 0 * anything = 0, Z flag set")
        void testMula_byZero_zFlagSet() {
            State s = freshState(12345, 0);
            Dep10MulDiv.MULA.exec(s, Mode.I);
            assertEquals(0, s.getA().value());
            assertTrue(s.getZ());
        }

        @Test
        @DisplayName("MULX: mirrors MULA behavior using X register")
        void testMulx_positiveResult_correctValue() {
            State s = freshState(5, 3);
            Dep10MulDiv.MULX.exec(s, Mode.I);
            assertEquals(15, s.getX().value());
        }
    }

    // MULHA/MULHX/UMULHA/UMULHX (the old high-word-only multiply) were retired
    // when Dep10MulDiv and Dep10MulDiv2 were merged (0.9.1) -- they no longer
    // exist as instructions. Getting a 32-bit product's high word is now
    // SMULA/SMULX (signed) or UMULA/UMULX (unsigned), which write the high
    // word to H in the same step as the low word to A/X. That's already
    // covered by SignedMultiply/UnsignedMultiply in MulDiv2Test.java, so there's
    // nothing to port forward here rather than delete.

    @Nested
    @DisplayName("DIVA / DIVX (signed divide)")
    class DivSigned {

        @Test
        @DisplayName("DIVA: 100 / 5 = 20, no flags")
        void testDiva_evenDivision_correctQuotient() {
            State s = freshState(100, 5);
            Dep10MulDiv.DIVA.exec(s, Mode.I);
            assertEquals(20, s.getA().value());
            assertFalse(s.getN());
            assertFalse(s.getZ());
            assertFalse(s.getV());
            assertFalse(s.getC());
        }

        @Test
        @DisplayName("DIVA: 5 / 100 = 0 (integer division), Z flag set")
        void testDiva_resultTruncatesToZero_zFlagSet() {
            State s = freshState(5, 100);
            Dep10MulDiv.DIVA.exec(s, Mode.I);
            assertEquals(0, s.getA().value());
            assertTrue(s.getZ());
        }

        @Test
        @DisplayName("DIVA: negative dividend gives negative quotient, N flag set")
        void testDiva_negativeDividend_nFlagSet() {
            State s = freshState(-100, 5);
            Dep10MulDiv.DIVA.exec(s, Mode.I);
            assertEquals(u16(-20), s.getA().value());
            assertTrue(s.getN());
        }

        @Test
        @DisplayName("DIVA: divide by zero sets C, does not crash")
        void testDiva_divideByZero_cFlagSetNoException() {
            State s = freshState(100, 0);
            assertDoesNotThrow(() -> Dep10MulDiv.DIVA.exec(s, Mode.I));
            assertTrue(s.getC());
            assertTrue(s.getV());
        }

        @Test
        @DisplayName("DIVA: divide by zero sets A to 0")
        void testDiva_divideByZero_registerSetToZero() {
            State s = freshState(12345, 0);
            Dep10MulDiv.DIVA.exec(s, Mode.I);
            assertEquals(0, s.getA().value(), "A is set to 0 on divide-by-zero (decided 9/15/2026)");
        }

        @Test
        @DisplayName("DIVA: MIN_VALUE / -1 overflow sets V flag")
        void testDiva_minValueDividedByNegativeOne_vFlagSet() {
            State s = freshState(Short.MIN_VALUE, -1);
            Dep10MulDiv.DIVA.exec(s, Mode.I);
            assertTrue(s.getV(), "the classic MIN_INT / -1 overflow case should set V");
        }

        @Test
        @DisplayName("DIVX: 100 / 5 = 20, no flags")
        void testDivx_evenDivision_correctQuotient() {
            State s = freshState(100, 5);
            Dep10MulDiv.DIVX.exec(s, Mode.I);
            assertEquals(20, s.getX().value());
            assertFalse(s.getN());
            assertFalse(s.getZ());
            assertFalse(s.getV());
            assertFalse(s.getC());
        }

        @Test
        @DisplayName("DIVX: 5 / 100 = 0 (integer division), Z flag set")
        void testDivx_resultTruncatesToZero_zFlagSet() {
            State s = freshState(5, 100);
            Dep10MulDiv.DIVX.exec(s, Mode.I);
            assertEquals(0, s.getX().value());
            assertTrue(s.getZ());
        }

        @Test
        @DisplayName("DIVX: negative dividend gives negative quotient, N flag set")
        void testDivx_negativeDividend_nFlagSet() {
            State s = freshState(-100, 5);
            Dep10MulDiv.DIVX.exec(s, Mode.I);
            assertEquals(u16(-20), s.getX().value());
            assertTrue(s.getN());
        }

        @Test
        @DisplayName("DIVX: divide by zero sets C, does not crash")
        void testDivx_divideByZero_cFlagSetNoException() {
            State s = freshState(100, 0);
            assertDoesNotThrow(() -> Dep10MulDiv.DIVX.exec(s, Mode.I));
            assertTrue(s.getC());
            assertTrue(s.getV());
        }

        @Test
        @DisplayName("DIVX: divide by zero sets X to 0")
        void testDivx_divideByZero_registerSetToZero() {
            State s = freshState(12345, 0);
            Dep10MulDiv.DIVX.exec(s, Mode.I);
            assertEquals(0, s.getX().value(), "X is set to 0 on divide-by-zero (decided 9/15/2026)");
        }

        @Test
        @DisplayName("DIVX: MIN_VALUE / -1 overflow sets V flag")
        void testDivx_minValueDividedByNegativeOne_vFlagSet() {
            State s = freshState(Short.MIN_VALUE, -1);
            Dep10MulDiv.DIVX.exec(s, Mode.I);
            assertTrue(s.getV(), "the classic MIN_INT / -1 overflow case should set V");
        }
    }

    @Nested
    @DisplayName("UDIVA / UDIVX (unsigned divide)")
    class DivUnsigned {

        @Test
        @DisplayName("UDIVA: 100 / 5 = 20")
        void testUdiva_evenDivision_correctQuotient() {
            State s = freshState(100, 5);
            Dep10MulDiv.UDIVA.exec(s, Mode.I);
            assertEquals(20, s.getA().value());
        }

        @Test
        @DisplayName("UDIVA: divide by zero sets C, does not crash")
        void testUdiva_divideByZero_cFlagSetNoException() {
            State s = freshState(100, 0);
            assertDoesNotThrow(() -> Dep10MulDiv.UDIVA.exec(s, Mode.I));
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("UDIVA: N reflects bit 15 of quotient (decided 9/12/2026)")
        void testUdiva_nFlag_reflectsHighBitOfResult() {
            State s = freshState(60000, 1);
            Dep10MulDiv.UDIVA.exec(s, Mode.I);
            assertTrue(s.getN(), "N should be set because quotient 60000 has bit 15 set");
        }

        @Test
        @DisplayName("UDIVA: N is clear when bit 15 of quotient is 0")
        void testUdiva_nFlag_clearWhenHighBitClear() {
            State s = freshState(100, 5);
            Dep10MulDiv.UDIVA.exec(s, Mode.I);
            assertFalse(s.getN(), "N should be clear because quotient 20 has bit 15 clear");
        }

        @Test
        @DisplayName("UDIVX: 100 / 5 = 20")
        void testUdivx_evenDivision_correctQuotient() {
            State s = freshState(100, 5);
            Dep10MulDiv.UDIVX.exec(s, Mode.I);
            assertEquals(20, s.getX().value());
        }

        @Test
        @DisplayName("UDIVX: divide by zero sets C, does not crash")
        void testUdivx_divideByZero_cFlagSetNoException() {
            State s = freshState(100, 0);
            assertDoesNotThrow(() -> Dep10MulDiv.UDIVX.exec(s, Mode.I));
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("UDIVX: N reflects bit 15 of quotient (decided 9/12/2026)")
        void testUdivx_nFlag_reflectsHighBitOfResult() {
            State s = freshState(60000, 1);
            Dep10MulDiv.UDIVX.exec(s, Mode.I);
            assertTrue(s.getN(), "N should be set because quotient 60000 has bit 15 set");
        }

        @Test
        @DisplayName("UDIVX: N is clear when bit 15 of quotient is 0")
        void testUdivx_nFlag_clearWhenHighBitClear() {
            State s = freshState(100, 5);
            Dep10MulDiv.UDIVX.exec(s, Mode.I);
            assertFalse(s.getN(), "N should be clear because quotient 20 has bit 15 clear");
        }

        @Test
        @DisplayName("UDIVX: divide by zero sets X to 0")
        void testUdivx_divideByZero_registerSetToZero() {
            State s = freshState(12345, 0);
            Dep10MulDiv.UDIVX.exec(s, Mode.I);
            assertEquals(0, s.getX().value(), "X is set to 0 on divide-by-zero (decided 9/15/2026)");
        }
    }

    @Nested
    @DisplayName("MODA / MODX (signed remainder)")
    class ModSigned {

        @Test
        @DisplayName("MODA: 17 % 5 = 2")
        void testModa_positiveOperands_correctRemainder() {
            State s = freshState(17, 5);
            Dep10MulDiv.MODA.exec(s, Mode.I);
            assertEquals(2, s.getA().value());
        }

        @Test
        @DisplayName("MODA: negative dividend gives negative remainder (Java semantics)")
        void testModa_negativeDividend_negativeRemainder() {
            State s = freshState(-17, 5);
            Dep10MulDiv.MODA.exec(s, Mode.I);
            assertEquals(u16(-2), s.getA().value());
            assertTrue(s.getN());
        }

        @Test
        @DisplayName("MODA: mod by zero sets C, does not crash")
        void testModa_byZero_cFlagSetNoException() {
            State s = freshState(17, 0);
            assertDoesNotThrow(() -> Dep10MulDiv.MODA.exec(s, Mode.I));
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("MODA: exact division gives remainder 0, Z flag set")
        void testModa_exactDivision_zFlagSet() {
            State s = freshState(20, 5);
            Dep10MulDiv.MODA.exec(s, Mode.I);
            assertEquals(0, s.getA().value());
            assertTrue(s.getZ());
        }

        @Test
        @DisplayName("MODX: 17 % 5 = 2")
        void testModx_positiveOperands_correctRemainder() {
            State s = freshState(17, 5);
            Dep10MulDiv.MODX.exec(s, Mode.I);
            assertEquals(2, s.getX().value());
        }

        @Test
        @DisplayName("MODX: negative dividend gives negative remainder (Java semantics)")
        void testModx_negativeDividend_negativeRemainder() {
            State s = freshState(-17, 5);
            Dep10MulDiv.MODX.exec(s, Mode.I);
            assertEquals(u16(-2), s.getX().value());
            assertTrue(s.getN());
        }

        @Test
        @DisplayName("MODX: mod by zero sets C, does not crash")
        void testModx_byZero_cFlagSetNoException() {
            State s = freshState(17, 0);
            assertDoesNotThrow(() -> Dep10MulDiv.MODX.exec(s, Mode.I));
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("MODX: exact division gives remainder 0, Z flag set")
        void testModx_exactDivision_zFlagSet() {
            State s = freshState(20, 5);
            Dep10MulDiv.MODX.exec(s, Mode.I);
            assertEquals(0, s.getX().value());
            assertTrue(s.getZ());
        }
    }

    @Nested
    @DisplayName("UMODA / UMODX (unsigned remainder)")
    class ModUnsigned {

        @Test
        @DisplayName("UMODA: 17 % 5 = 2")
        void testUmoda_positiveOperands_correctRemainder() {
            State s = freshState(17, 5);
            Dep10MulDiv.UMODA.exec(s, Mode.I);
            assertEquals(2, s.getA().value());
        }

        @Test
        @DisplayName("UMODA: N can be set -- e.g. 65534 % 65535 = 65534, bit 15 set (correction per Brian, 9/15/2026)")
        void testUmoda_nFlag_canBeSetWhenDivisorExceedsDividend() {
            // Earlier reasoning wrongly assumed the divisor is always <= the
            // dividend. When the divisor is larger, a % op == a unchanged
            // (untouched by the divide at all), which can be as large as
            // 65534 -- well past the 32767 boundary, with bit 15 set.
            State s = freshState(65534, 65535);
            Dep10MulDiv.UMODA.exec(s, Mode.I);
            assertEquals(65534, s.getA().value());
            assertTrue(s.getN(), "N should be set: 65534 has bit 15 set");
        }

        @Test
        @DisplayName("UMODA: N is clear for a small remainder (sanity check, unrelated to the bit-15 boundary)")
        void testUmoda_nFlag_clearForSmallRemainder() {
            State s = freshState(65535, 32768);
            Dep10MulDiv.UMODA.exec(s, Mode.I);
            assertEquals(32767, s.getA().value());
            assertFalse(s.getN());
        }

        @Test
        @DisplayName("UMODA: mod by zero sets C, does not crash")
        void testUmoda_byZero_cFlagSetNoException() {
            State s = freshState(17, 0);
            assertDoesNotThrow(() -> Dep10MulDiv.UMODA.exec(s, Mode.I));
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("UMODX: 17 % 5 = 2")
        void testUmodx_positiveOperands_correctRemainder() {
            State s = freshState(17, 5);
            Dep10MulDiv.UMODX.exec(s, Mode.I);
            assertEquals(2, s.getX().value());
        }

        @Test
        @DisplayName("UMODX: N can be set -- e.g. 65534 % 65535 = 65534, bit 15 set (correction per Brian, 9/15/2026)")
        void testUmodx_nFlag_canBeSetWhenDivisorExceedsDividend() {
            State s = freshState(65534, 65535);
            Dep10MulDiv.UMODX.exec(s, Mode.I);
            assertEquals(65534, s.getX().value());
            assertTrue(s.getN(), "N should be set: 65534 has bit 15 set");
        }

        @Test
        @DisplayName("UMODX: N is clear for a small remainder (sanity check, unrelated to the bit-15 boundary)")
        void testUmodx_nFlag_clearForSmallRemainder() {
            State s = freshState(65535, 32768);
            Dep10MulDiv.UMODX.exec(s, Mode.I);
            assertEquals(32767, s.getX().value());
            assertFalse(s.getN());
        }

        @Test
        @DisplayName("UMODX: mod by zero sets C, does not crash")
        void testUmodx_byZero_cFlagSetNoException() {
            State s = freshState(17, 0);
            assertDoesNotThrow(() -> Dep10MulDiv.UMODX.exec(s, Mode.I));
            assertTrue(s.getC());
        }
    }
}
