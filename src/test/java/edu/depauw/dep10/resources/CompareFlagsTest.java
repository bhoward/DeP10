package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static edu.depauw.dep10.resources.PepHarness.mem;

/**
 * Checks the N, Z, V, C flags after CPWA and SUBA against Java arithmetic,
 * including operand 0x8000, whose two's complement negation is itself.
 */
class CompareFlagsTest {

    private static final int[] VALS = { 0, 1, 2, 0x7FFE, 0x7FFF, 0x8000, 0x8001, 0xFFFE, 0xFFFF };

    private static int flags(String op, int a, int b) {
        State s = PepHarness.run(String.format("""
                LDWA    0x%04X,i
                %s      0x%04X,i
                MOVFLGA
                STWA    0xF000,d
                LDBA    0,i
                STBA    pwrOff,d
                .END
                """, a, op, b));
        return mem(s, 0xF000) & 0xF;
    }

    private static int expected(int a, int b) {
        int diff = (short) a - (short) b;
        boolean v = diff != (short) diff;
        boolean z = ((a - b) & 0xFFFF) == 0;
        boolean n = ((a - b) & 0x8000) != 0;
        boolean c = a >= b;
        return (n ^ v ? 8 : 0) | 0 | (z ? 4 : 0) | (v ? 2 : 0) | (c ? 1 : 0);
    }

    @Test
    @DisplayName("CPWA sets N (as sign xor overflow), Z, V and C correctly for all edge values")
    void cpwa() {
        for (int a : VALS) {
            for (int b : VALS) {
                assertEquals(expected(a, b), flags("CPWA", a, b), String.format("CPWA 0x%04X, 0x%04X", a, b));
            }
        }
    }

    @Test
    @DisplayName("SUBA sets Z, V and C correctly for all edge values")
    void suba() {
        for (int a : VALS) {
            for (int b : VALS) {
                int diff = (short) a - (short) b;
                boolean v = diff != (short) diff;
                boolean z = ((a - b) & 0xFFFF) == 0;
                boolean n = ((a - b) & 0x8000) != 0;
                int e = (n ? 8 : 0) | (z ? 4 : 0) | (v ? 2 : 0) | (a >= b ? 1 : 0);
                assertEquals(e, flags("SUBA", a, b), String.format("SUBA 0x%04X, 0x%04X", a, b));
            }
        }
    }
}
