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
            Pep10.MULA.exec(s, Mode.I);

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
            Pep10.MULA.exec(s, Mode.I);

            assertEquals(u16(-15), s.getA().value(), "low 16 bits of product should be -15");
            assertTrue(s.getN(), "N flag should be set because the result (-15) is negative");
        }

        @Test
        @DisplayName("BUG: MULA: 256 * 256 = 65536, low word is 0, Z flag should be set but is not")
        void testMula_lowWordZeroButFullProductNonzero_zFlagShouldBeSet() {
            State s = freshState(256, 256);
            Pep10.MULA.exec(s, Mode.I);

            assertEquals(0, s.getA().value(), "low 16 bits of 65536 should be 0");
            assertTrue(s.getZ(), "Z flag should be set because the stored result is 0");
        }

        @Test
        @DisplayName("MULA: carry set when product exceeds 16 bits")
        void testMula_largeProduct_carrySet() {
            State s = freshState(300, 40);
            Pep10.MULA.exec(s, Mode.I);
            assertEquals(12000, s.getA().value());
            assertFalse(s.getC());
        }

        @Test
        @DisplayName("MULA: 0 * anything = 0, Z flag set")
        void testMula_byZero_zFlagSet() {
            State s = freshState(12345, 0);
            Pep10.MULA.exec(s, Mode.I);
            assertEquals(0, s.getA().value());
            assertTrue(s.getZ());
        }

        @Test
        @DisplayName("MULX: mirrors MULA behavior using X register")
        void testMulx_positiveResult_correctValue() {
            State s = freshState(5, 3);
            Pep10.MULX.exec(s, Mode.I);
            assertEquals(15, s.getX().value());
        }
    }

    @Nested
    @DisplayName("MULHA / MULHX (signed high-word multiply)")
    class MulHighSigned {

        @Test
        @DisplayName("MULHA: small operands, high word is 0")
        void testMulha_smallOperands_highWordZero() {
            State s = freshState(5, 3);
            Pep10.MULHA.exec(s, Mode.I);
            assertEquals(0, s.getA().value());
            assertTrue(s.getZ());
        }

        @Test
        @DisplayName("MULHA: large operands produce nonzero high word")
        void testMulha_largeOperands_nonzeroHighWord() {
            State s = freshState(30000, 3);
            Pep10.MULHA.exec(s, Mode.I);
            assertEquals(1, s.getA().value());
            assertFalse(s.getZ());
        }

        @Test
        @DisplayName("MULHA: negative * positive gives negative high word")
        void testMulha_negativeTimesPositive_negativeHighWord() {
            State s = freshState(-30000, 3);
            Pep10.MULHA.exec(s, Mode.I);
            assertTrue(s.getN(), "high word of a negative product should be negative");
        }

        @Test
        @DisplayName("OPEN QUESTION: MULHA leaves V and C hardcoded false -- confirm intended")
        void testMulha_vAndCFlags_currentlyAlwaysFalse() {
            State s = freshState(30000, 30000);
            Pep10.MULHA.exec(s, Mode.I);
            assertFalse(s.getV(), "current implementation always clears V for MULHA");
            assertFalse(s.getC(), "current implementation always clears C for MULHA");
        }
    }

    @Nested
    @DisplayName("UMULHA / UMULHX (unsigned high-word multiply)")
    class MulHighUnsigned {

        @Test
        @DisplayName("UMULHA: large unsigned operands produce nonzero high word")
        void testUmulha_largeOperands_nonzeroHighWord() {
            State s = freshState(60000, 60000);
            Pep10.UMULHA.exec(s, Mode.I);
            assertEquals(0xD693, s.getA().value());
        }

        @Test
        @DisplayName("OPEN QUESTION: UMULHA sets N based on bit 15 of an unsigned result -- confirm intended")
        void testUmulha_nFlag_currentlySetFromBit15EvenThoughUnsigned() {
            State s = freshState(60000, 60000);
            Pep10.UMULHA.exec(s, Mode.I);
            assertTrue(s.getN(), "current implementation sets N from bit 15 even for unsigned result");
        }
    }

    @Nested
    @DisplayName("DIVA / DIVX (signed divide)")
    class DivSigned {

        @Test
        @DisplayName("DIVA: 100 / 5 = 20, no flags")
        void testDiva_evenDivision_correctQuotient() {
            State s = freshState(100, 5);
            Pep10.DIVA.exec(s, Mode.I);
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
            Pep10.DIVA.exec(s, Mode.I);
            assertEquals(0, s.getA().value());
            assertTrue(s.getZ());
        }

        @Test
        @DisplayName("DIVA: negative dividend gives negative quotient, N flag set")
        void testDiva_negativeDividend_nFlagSet() {
            State s = freshState(-100, 5);
            Pep10.DIVA.exec(s, Mode.I);
            assertEquals(u16(-20), s.getA().value());
            assertTrue(s.getN());
        }

        @Test
        @DisplayName("DIVA: divide by zero sets C, does not crash")
        void testDiva_divideByZero_cFlagSetNoException() {
            State s = freshState(100, 0);
            assertDoesNotThrow(() -> Pep10.DIVA.exec(s, Mode.I));
            assertTrue(s.getC());
            assertFalse(s.getV());
        }

        @Test
        @DisplayName("CHECK: DIVA divide by zero leaves A register unchanged -- confirm this is intended")
        void testDiva_divideByZero_registerUnchanged() {
            State s = freshState(12345, 0);
            Pep10.DIVA.exec(s, Mode.I);
            assertEquals(12345, s.getA().value(), "A is left at its pre-divide value on div-by-zero");
        }

        @Test
        @DisplayName("DIVA: MIN_VALUE / -1 overflow sets V flag")
        void testDiva_minValueDividedByNegativeOne_vFlagSet() {
            State s = freshState(Short.MIN_VALUE, -1);
            Pep10.DIVA.exec(s, Mode.I);
            assertTrue(s.getV(), "the classic MIN_INT / -1 overflow case should set V");
        }

        @Test
        @DisplayName("DIVX: 100 / 5 = 20, no flags")
        void testDivx_evenDivision_correctQuotient() {
            State s = freshState(100, 5);
            Pep10.DIVX.exec(s, Mode.I);
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
            Pep10.DIVX.exec(s, Mode.I);
            assertEquals(0, s.getX().value());
            assertTrue(s.getZ());
        }

        @Test
        @DisplayName("DIVX: negative dividend gives negative quotient, N flag set")
        void testDivx_negativeDividend_nFlagSet() {
            State s = freshState(-100, 5);
            Pep10.DIVX.exec(s, Mode.I);
            assertEquals(u16(-20), s.getX().value());
            assertTrue(s.getN());
        }

        @Test
        @DisplayName("DIVX: divide by zero sets C, does not crash")
        void testDivx_divideByZero_cFlagSetNoException() {
            State s = freshState(100, 0);
            assertDoesNotThrow(() -> Pep10.DIVX.exec(s, Mode.I));
            assertTrue(s.getC());
            assertFalse(s.getV());
        }

        @Test
        @DisplayName("CHECK: DIVX divide by zero leaves X register unchanged -- confirm this is intended")
        void testDivx_divideByZero_registerUnchanged() {
            State s = freshState(12345, 0);
            Pep10.DIVX.exec(s, Mode.I);
            assertEquals(12345, s.getX().value(), "X is left at its pre-divide value on div-by-zero");
        }

        @Test
        @DisplayName("DIVX: MIN_VALUE / -1 overflow sets V flag")
        void testDivx_minValueDividedByNegativeOne_vFlagSet() {
            State s = freshState(Short.MIN_VALUE, -1);
            Pep10.DIVX.exec(s, Mode.I);
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
            Pep10.UDIVA.exec(s, Mode.I);
            assertEquals(20, s.getA().value());
        }

        @Test
        @DisplayName("UDIVA: divide by zero sets C, does not crash")
        void testUdiva_divideByZero_cFlagSetNoException() {
            State s = freshState(100, 0);
            assertDoesNotThrow(() -> Pep10.UDIVA.exec(s, Mode.I));
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("OPEN QUESTION: UDIVA forces N false regardless of bit 15 -- confirm intended")
        void testUdiva_nFlag_currentlyAlwaysFalse() {
            State s = freshState(60000, 1);
            Pep10.UDIVA.exec(s, Mode.I);
            assertFalse(s.getN(), "current implementation hardcodes N to false for UDIVA");
        }

        @Test
        @DisplayName("UDIVX: 100 / 5 = 20")
        void testUdivx_evenDivision_correctQuotient() {
            State s = freshState(100, 5);
            Pep10.UDIVX.exec(s, Mode.I);
            assertEquals(20, s.getX().value());
        }

        @Test
        @DisplayName("UDIVX: divide by zero sets C, does not crash")
        void testUdivx_divideByZero_cFlagSetNoException() {
            State s = freshState(100, 0);
            assertDoesNotThrow(() -> Pep10.UDIVX.exec(s, Mode.I));
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("OPEN QUESTION: UDIVX forces N false regardless of bit 15 -- confirm intended")
        void testUdivx_nFlag_currentlyAlwaysFalse() {
            State s = freshState(60000, 1);
            Pep10.UDIVX.exec(s, Mode.I);
            assertFalse(s.getN(), "current implementation hardcodes N to false for UDIVX");
        }

        @Test
        @DisplayName("CHECK: UDIVX divide by zero leaves X register unchanged -- confirm this is intended")
        void testUdivx_divideByZero_registerUnchanged() {
            State s = freshState(12345, 0);
            Pep10.UDIVX.exec(s, Mode.I);
            assertEquals(12345, s.getX().value(), "X is left at its pre-divide value on div-by-zero");
        }
    }

    @Nested
    @DisplayName("MODA / MODX (signed remainder)")
    class ModSigned {

        @Test
        @DisplayName("MODA: 17 % 5 = 2")
        void testModa_positiveOperands_correctRemainder() {
            State s = freshState(17, 5);
            Pep10.MODA.exec(s, Mode.I);
            assertEquals(2, s.getA().value());
        }

        @Test
        @DisplayName("MODA: negative dividend gives negative remainder (Java semantics)")
        void testModa_negativeDividend_negativeRemainder() {
            State s = freshState(-17, 5);
            Pep10.MODA.exec(s, Mode.I);
            assertEquals(u16(-2), s.getA().value());
            assertTrue(s.getN());
        }

        @Test
        @DisplayName("MODA: mod by zero sets C, does not crash")
        void testModa_byZero_cFlagSetNoException() {
            State s = freshState(17, 0);
            assertDoesNotThrow(() -> Pep10.MODA.exec(s, Mode.I));
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("MODA: exact division gives remainder 0, Z flag set")
        void testModa_exactDivision_zFlagSet() {
            State s = freshState(20, 5);
            Pep10.MODA.exec(s, Mode.I);
            assertEquals(0, s.getA().value());
            assertTrue(s.getZ());
        }

        @Test
        @DisplayName("MODX: 17 % 5 = 2")
        void testModx_positiveOperands_correctRemainder() {
            State s = freshState(17, 5);
            Pep10.MODX.exec(s, Mode.I);
            assertEquals(2, s.getX().value());
        }

        @Test
        @DisplayName("MODX: negative dividend gives negative remainder (Java semantics)")
        void testModx_negativeDividend_negativeRemainder() {
            State s = freshState(-17, 5);
            Pep10.MODX.exec(s, Mode.I);
            assertEquals(u16(-2), s.getX().value());
            assertTrue(s.getN());
        }

        @Test
        @DisplayName("MODX: mod by zero sets C, does not crash")
        void testModx_byZero_cFlagSetNoException() {
            State s = freshState(17, 0);
            assertDoesNotThrow(() -> Pep10.MODX.exec(s, Mode.I));
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("MODX: exact division gives remainder 0, Z flag set")
        void testModx_exactDivision_zFlagSet() {
            State s = freshState(20, 5);
            Pep10.MODX.exec(s, Mode.I);
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
            Pep10.UMODA.exec(s, Mode.I);
            assertEquals(2, s.getA().value());
        }

        @Test
        @DisplayName("UMODA: mod by zero sets C, does not crash")
        void testUmoda_byZero_cFlagSetNoException() {
            State s = freshState(17, 0);
            assertDoesNotThrow(() -> Pep10.UMODA.exec(s, Mode.I));
            assertTrue(s.getC());
        }

        @Test
        @DisplayName("UMODX: 17 % 5 = 2")
        void testUmodx_positiveOperands_correctRemainder() {
            State s = freshState(17, 5);
            Pep10.UMODX.exec(s, Mode.I);
            assertEquals(2, s.getX().value());
        }

        @Test
        @DisplayName("UMODX: mod by zero sets C, does not crash")
        void testUmodx_byZero_cFlagSetNoException() {
            State s = freshState(17, 0);
            assertDoesNotThrow(() -> Pep10.UMODX.exec(s, Mode.I));
            assertTrue(s.getC());
        }
    }
}
