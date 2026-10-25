package edu.depauw.dep10.resources;

import static edu.depauw.dep10.resources.PepHarness.mem;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.depauw.dep10.simulator.State;

/**
 * End-to-end tests for FAdd (src/main/resources/float16.pep), pulled in with
 * .INCLUDELIB the same way a student or another routine would use them.
 */
class FAddTest {

    private static State run(String driverAndChecks) {
        return PepHarness.run(driverAndChecks + "\n.INCLUDELIB \"float16\"\n");
    }

    private static final int ZERO = 0x0000,
            NEG_ZERO = 0x8000,
            HALF = 0x3800,
            NEG_HALF = 0xB800,
            ONE = 0x3C00,
            NEG_ONE = 0xBC00,
            TWO = 0x4000,
            NEG_TWO = 0xC000,
            THREE = 0x4200,
            NEG_THREE = 0xC200,
            FIVE = 0x4500,
            NEG_FIVE = 0xC500,
            INFINITY = 0x7C00,
            NEG_INFINITY = 0xFC00,
            NAN = 0x7FFF, // one of them...
            NEG_NAN = 0xFFFF,
            BIG = 0x7BFF, // largest normal
            NEG_BIG = 0xFBFF,
            SMALL = 0x0400, // smallest normal
            NEG_SMALL = 0x8400,
            SMALLER = 0x03FF, // largest subnormal
            NEG_SMALLER = 0x83FF,
            TINY = 0x0001, // smallest nonzero
            NEG_TINY = 0x8001;

    private static final int[][] CASES = {
            { ZERO, ZERO, ZERO },
            { ZERO, ONE, ONE },
            { ZERO, NEG_ONE, NEG_ONE },
            { ZERO, FIVE, FIVE },
            { ZERO, INFINITY, INFINITY },
            { ZERO, NEG_INFINITY, NEG_INFINITY },
            { ZERO, NAN, NAN },
            { ZERO, NEG_NAN, NEG_NAN },
            { ZERO, BIG, BIG },
            { ZERO, SMALL, SMALL },
//            { ZERO, NEG_SMALLER, NEG_SMALLER },
//            { ZERO, TINY, TINY },
            { NEG_ZERO, ONE, ONE },
            { HALF, HALF, ONE },
            { HALF, NEG_HALF, ZERO },
            { ONE, ONE, TWO },
            { ONE, TWO, THREE },
            { TWO, THREE, FIVE },
            { TWO, NEG_THREE, NEG_ONE },
            { FIVE, NEG_TWO, THREE },
            { NEG_FIVE, INFINITY, INFINITY },
            { BIG, NEG_BIG, ZERO },
            { BIG, BIG, INFINITY },
            { SMALLER, ONE, ONE },
//            { NEG_SMALLER, NEG_SMALLER, NEG_SMALL },
            { TINY, NEG_TINY, ZERO },
            { INFINITY, NEG_INFINITY, NAN },
    };

    @Test
    @DisplayName("FAdd: Common and edge cases")
    void dadd_cases() {
        for (var c : CASES) {
            State s = run(String.format("""
                           LDBX    ' ',i
                           LDWA    0x%04X,i
                           PUSHA
                           CALL    FPrint,i
                           STBX    charOut,d
                           LDWA    0x%04X,i
                           PUSHA
                           CALL    FPrint,i
                           LDBX    ' ',i
                           STBX    charOut,d
                           CALL    FAdd,i
                           POPA
                           CALL    FPrint,i
                           LDBX    '\\n',i
                           STBX    charOut,d
                           POPA
                           STWA    0xF000,d
                           LDBA    0,i
                           STBA    pwrOff,d
                    """, c[0], c[1]));
            assertEquals(c[2], mem(s, 0xF000), "0x%04X + 0x%04X = 0x%04X".formatted(c[0], c[1], c[2]));
        }

    }
}
