package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static edu.depauw.dep10.resources.PepHarness.mem;

/**
 * Assembles and runs small programs that use the extension instructions
 * (ADCr, SBCr, SXWr, SXBr, TSTr, LEAr, SWAPr) through the real assembler and
 * simulator, so the mnemonics, opcode encodings, and semantics are checked together.
 */
class Dep10ExtProgramTest {

    private static State run(String body) {
        return PepHarness.run(body + "\n        LDBA    0,i\n        STBA    pwrOff,d\n        .END\n");
    }

    private static final int[] VALS = { 0, 1, 2, 0x7FFF, 0x8000, 0xFFFF, 0x10000, 0x7FFFFFFF, 0x80000000,
            0xFFFFFFFF, 70000, 0x0001FFFF, 0xFFFF0000, 123456789 };

    @Test
    @DisplayName("32-bit add built from ADDA + ADCA matches Java int addition")
    void add32() {
        for (int a : VALS) {
            for (int b : VALS) {
                State s = run(String.format("""
                        LDWA    0x%04X,i
                        ADDA    0x%04X,i
                        STWA    0xF002,d
                        LDWA    0x%04X,i
                        ADCA    0x%04X,i
                        STWA    0xF000,d
                        """, a & 0xFFFF, b & 0xFFFF, a >>> 16, b >>> 16));
                int e = a + b;
                assertEquals(e >>> 16, mem(s, 0xF000), a + "+" + b + " high");
                assertEquals(e & 0xFFFF, mem(s, 0xF002), a + "+" + b + " low");
            }
        }
    }

    @Test
    @DisplayName("32-bit subtract built from SUBA + SBCA matches Java int subtraction")
    void sub32() {
        for (int a : VALS) {
            for (int b : VALS) {
                State s = run(String.format("""
                        LDWA    0x%04X,i
                        SUBA    0x%04X,i
                        STWA    0xF002,d
                        LDWA    0x%04X,i
                        SBCA    0x%04X,i
                        STWA    0xF000,d
                        """, a & 0xFFFF, b & 0xFFFF, a >>> 16, b >>> 16));
                int e = a - b;
                assertEquals(e >>> 16, mem(s, 0xF000), a + "-" + b + " high");
                assertEquals(e & 0xFFFF, mem(s, 0xF002), a + "-" + b + " low");
            }
        }
    }

    @Test
    @DisplayName("X versions: 32-bit add with ADDX + ADCX")
    void add32WithX() {
        State s = run("""
                LDWX    0xFFFF,i
                ADDX    1,i
                STWX    0xF002,d
                LDWX    0x0001,i
                ADCX    0,i
                STWX    0xF000,d
                """);
        assertEquals(2, mem(s, 0xF000));
        assertEquals(0, mem(s, 0xF002));
    }

    @Test
    @DisplayName("SXWA widens a 16-bit INTEGER to a 32-bit value in H:A")
    void widen() {
        Random rnd = new Random(5);
        for (int i = 0; i < 30; i++) {
            int v = (short) rnd.nextInt(0x10000);
            State s = run(String.format("""
                    LDWA    0x%04X,i
                    SXWA
                    STWA    0xF002,d
                    SWAPHA
                    STWA    0xF000,d
                    """, v & 0xFFFF));
            assertEquals((v >>> 16) & 0xFFFF, mem(s, 0xF000), v + " high");
            assertEquals(v & 0xFFFF, mem(s, 0xF002), v + " low");
        }
    }

    @Test
    @DisplayName("SXBA and SXBX sign-extend bytes")
    void signExtendBytes() {
        State s = run("""
                LDWA    0x1280,i
                SXBA
                STWA    0xF000,d
                LDWX    0x007F,i
                SXBX
                STWX    0xF002,d
                LDWX    0x00FF,i
                SXBX
                STWX    0xF004,d
                """);
        assertEquals(0xFF80, mem(s, 0xF000));
        assertEquals(0x007F, mem(s, 0xF002));
        assertEquals(0xFFFF, mem(s, 0xF004));
    }

    @Test
    @DisplayName("TSTA lets a branch test a register without a compare")
    void tstBranches() {
        State s = run("""
                LDWA    0,i
                TSTA
                BRNE    bad
                LDWA    5,i
                STWA    0xF000,d
                LDWX    0xFFFF,i
                TSTX
                BRGE    bad
                LDWA    6,i
                STWA    0xF002,d
                BR      done
        bad:    LDWA    0xBAD,i
                STWA    0xF000,d
        done:   NOP
                """);
        assertEquals(5, mem(s, 0xF000));
        assertEquals(6, mem(s, 0xF002));
    }

    @Test
    @DisplayName("LEAA gives the address of a local variable, which can be used through ,n")
    void leaOfLocal() {
        State s = run("""
                SUBSP   4,i
                LDWA    1234,i
                STWA    2,s
                LEAX    2,s
                LDWA    77,i
                STWA    0,x
                LDWA    2,s
                STWA    0xF000,d
                LEAA    2,s
                STWA    0xF002,d
                STWX    0xF004,d
                ADDSP   4,i
                """);
        assertEquals(77, mem(s, 0xF000), "store through the pointer changed the local");
        assertEquals(mem(s, 0xF004), mem(s, 0xF002), "LEAA and LEAX agree on the address");
    }

    @Test
    @DisplayName("SWAPA and SWAPX exchange a register with memory")
    void swapMemory() {
        State s = run("""
                LDWA    111,i
                STWA    0xF000,d
                LDWA    222,i
                SWAPA   0xF000,d
                STWA    0xF002,d
                LDWX    333,i
                SWAPX   0xF000,d
                STWX    0xF004,d
                """);
        assertEquals(333, mem(s, 0xF000));
        assertEquals(111, mem(s, 0xF002));
        assertEquals(222, mem(s, 0xF004));
    }
}
