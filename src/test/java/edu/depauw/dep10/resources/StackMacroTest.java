package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static edu.depauw.dep10.resources.PepHarness.mem;

/**
 * End-to-end tests for the NIP and DNIP macros in src/main/resources/stack.pep.
 * Both must leave X alone, since compiled code keeps its frame pointer there.
 */
class StackMacroTest {

    private static State run(String body) {
        return PepHarness.run(".INCLUDELIB \"stack\"\n" + body + "\n        .END\n");
    }

    @Test
    @DisplayName("NIP discards the second word and keeps the top one")
    void nip() {
        State s = run("""
                LDWX    0x1234,i
                LDWA    0x0AAA,i
                PUSHA
                LDWA    0x1111,i
                PUSHA
                LDWA    0x2222,i
                PUSHA
                @NIP
                POPA
                STWA    0xF000,d
                POPA
                STWA    0xF002,d
                STWX    0xF004,d
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0x2222, mem(s, 0xF000));
        assertEquals(0x0AAA, mem(s, 0xF002));
        assertEquals(0x1234, mem(s, 0xF004));
    }

    @Test
    @DisplayName("DNIP discards the second 32-bit value and keeps the top one")
    void dnip() {
        State s = run("""
                LDWX    0x4321,i
                LDWA    0x0BBB,i
                PUSHA
                LDWA    0x1001,i
                PUSHA
                LDWA    0x1002,i
                PUSHA
                LDWA    0x2001,i
                PUSHA
                LDWA    0x2002,i
                PUSHA
                @DNIP
                POPA
                STWA    0xF000,d
                POPA
                STWA    0xF002,d
                POPA
                STWA    0xF004,d
                STWX    0xF006,d
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0x2002, mem(s, 0xF000));
        assertEquals(0x2001, mem(s, 0xF002));
        assertEquals(0x0BBB, mem(s, 0xF004));
        assertEquals(0x4321, mem(s, 0xF006));
    }
}
