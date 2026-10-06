package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static edu.depauw.dep10.resources.PepHarness.mem;

/**
 * End-to-end tests for DMul (src/main/resources/dmul.pep), pulled in with
 * .INCLUDELIB the same way a student or another routine would use it.
 *
 * Calling convention matches DADD/DSUB/DDiv/UDDiv: operands are pushed low
 * word then high word, A first and then B. The (truncated 32-bit) product
 * overwrites the FIRST-pushed operand's slot, so ADDSP 4,i right after CALL
 * discards B's now-unused slot and leaves the product ready to chain.
 *
 * Since DMul keeps only the low 32 bits of the product, the signed and
 * unsigned results are identical, so these tests use two's complement
 * values for negatives.
 */
class DMulTest {

    private static State run(String driverAndChecks) {
        return PepHarness.run(driverAndChecks + "\n.INCLUDELIB \"dmul\"\n        .END\n");
    }

    /** Multiplies two 32-bit values and leaves hi/lo of the product at 0xF000/0xF002. */
    private static State mul(int a, int b) {
        return run(String.format("""
                LDWA    0x%04X,i        ; A low
                PUSHA
                LDWA    0x%04X,i        ; A high
                PUSHA
                LDWA    0x%04X,i        ; B low
                PUSHA
                LDWA    0x%04X,i        ; B high
                PUSHA
                CALL    DMul,i
                ADDSP   4,i
                POPA
                STWA    0xF000,d
                POPA
                STWA    0xF002,d
                LDBA    0,i
                STBA    pwrOff,d
                """, a & 0xFFFF, (a >>> 16) & 0xFFFF, b & 0xFFFF, (b >>> 16) & 0xFFFF));
    }

    private static void check(int a, int b) {
        int expected = a * b; // Java int multiplication is exactly low-32-bit truncation
        State s = mul(a, b);
        String msg = a + " * " + b;
        assertEquals((expected >>> 16) & 0xFFFF, mem(s, 0xF000), msg + " high word");
        assertEquals(expected & 0xFFFF, mem(s, 0xF002), msg + " low word");
    }

    @Test
    @DisplayName("DMul: 5 * 3 = 15")
    void dmul_simple() {
        check(5, 3);
    }

    @Test
    @DisplayName("DMul: multiply by zero and by one")
    void dmul_zero_and_one() {
        check(123456, 0);
        check(0, 123456);
        check(123456, 1);
        check(1, 123456);
    }

    @Test
    @DisplayName("DMul: low-word product carries into the high word (65535 * 65535)")
    void dmul_low_word_carry() {
        check(0xFFFF, 0xFFFF);
        check(0x10000 - 1, 2);
    }

    @Test
    @DisplayName("DMul: operands with nonzero high words, cross terms")
    void dmul_cross_terms() {
        check(0x00010001, 0x00010001); // (2^16 + 1)^2: cross terms land in the high word
        check(70000, 5);
        check(70000, 70000);           // overflows 32 bits, must truncate
        check(0x12345678, 0x0000009A);
        check(0x0000009A, 0x12345678);
    }

    @Test
    @DisplayName("DMul: negative operands (two's complement)")
    void dmul_negatives() {
        check(-1, 1);
        check(-1, -1);
        check(-5, 3);
        check(5, -3);
        check(-70000, 5);
        check(-70000, -70000);
    }

    @Test
    @DisplayName("DMul: extremes (INT32_MAX, INT32_MIN)")
    void dmul_extremes() {
        check(Integer.MAX_VALUE, 2);
        check(Integer.MAX_VALUE, Integer.MAX_VALUE);
        check(Integer.MIN_VALUE, 1);
        check(Integer.MIN_VALUE, -1);
        check(Integer.MIN_VALUE, 2);
    }

    @Test
    @DisplayName("DMul: commutative on a spread of values")
    void dmul_commutative() {
        int[] vals = { 3, 255, 256, 4464, 65535, 65536, 100000, 0x7FFFFFFF, -2, -4464 };
        for (int a : vals) {
            for (int b : vals) {
                check(a, b);
            }
        }
    }

    @Test
    @DisplayName("DMul: chained A * B * C, 70000 * 3 * 5 = 1050000")
    void dmul_chains() {
        State s = run("""
                LDWA    4464,i          ; A = 70000 = 0x00011170, low
                PUSHA
                LDWA    1,i             ; A high
                PUSHA
                LDWA    3,i             ; B = 3
                PUSHA
                LDWA    0,i
                PUSHA
                CALL    DMul,i
                ADDSP   4,i
                LDWA    5,i             ; C = 5
                PUSHA
                LDWA    0,i
                PUSHA
                CALL    DMul,i
                ADDSP   4,i
                POPA
                STWA    0xF010,d
                POPA
                STWA    0xF012,d
                LDBA    0,i
                STBA    pwrOff,d
                """);
        int expected = 70000 * 3 * 5;
        assertEquals((expected >>> 16) & 0xFFFF, mem(s, 0xF010), "chained high word");
        assertEquals(expected & 0xFFFF, mem(s, 0xF012), "chained low word");
    }
}
