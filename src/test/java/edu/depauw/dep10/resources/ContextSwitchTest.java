package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static edu.depauw.dep10.resources.PepHarness.mem;

/**
 * First milestone of the OS concurrency design: SCALL pushes a 12-byte frame
 * (NZVC, A, X, PC, SP, instruction) on the system stack whose top is
 * Mem[FFFB], and SRET pops one, so a context switch needs no register-saving
 * code. Two hand-built processes take turns through a trap handler that only
 * swaps the stack pointer between two saved frames before executing SRET.
 *
 * The program installs its own trap handler by writing Mem[FFF7], and sets
 * Mem[FFFB] to the top of process A's system stack before A makes its first
 * call. Each process has its own user stack and its own system stack.
 */
class ContextSwitchTest {

    private static final String PROGRAM = """
            BR      main
    cur:    .WORD   0               ; 0 = A is running, 1 = B is running
    pcbA:   .WORD   0               ; saved frame address of A
    pcbB:   .WORD   0               ; saved frame address of B
    logIdx: .WORD   0
    log:     .EQUATE 0xF100
    userA:  .BLOCK  32
    userAT: .WORD   0               ; top of A's user stack is the label userAT
    userB:  .BLOCK  32
    userBT: .WORD   0
    sysA:   .BLOCK  64
    sysAT:  .WORD   0
    sysB:   .BLOCK  64
    sysBT:  .WORD   0

    ; yield handler: swap the saved frame pointers, then SRET
    handler: MOVSPA
            LDWX    cur,d
            BRNE    fromB
            STWA    pcbA,d
            LDWA    1,i
            STWA    cur,d
            LDWA    pcbB,d
            MOVASP
            SRET
    fromB:  STWA    pcbB,d
            LDWA    0,i
            STWA    cur,d
            LDWA    pcbA,d
            MOVASP
            SRET

    ; a fake first frame for B at the top of its system stack
    main:   LDWA    handler,i
            STWA    0xFFF7,d        ; trap handler vector
            LDWA    sysBT,i
            SUBA    12,i
            STWA    pcbB,d
            LDWX    pcbB,d
            LDWA    procB,i
            STWA    5,x             ; saved PC
            LDWA    userBT,i
            STWA    7,x             ; saved SP
            LDWA    sysAT,i
            STWA    0xFFFB,d        ; system stack top for A's first call
            LDWA    userAT,i
            MOVASP
            BR      procA

    ; process A logs 1, yields, and stops after its third turn
    procA:
    aLoop:  LDWX    logIdx,d
            LDWA    1,i
            STWA    log,x
            LDWA    logIdx,d
            ADDA    2,i
            STWA    logIdx,d
            LDWA    0x00A1,i
            LDWX    0x1111,i
            SCALL   0,i
            STWA    0xF000,d        ; A after the switches (must still be 0x00A1)
            STWX    0xF002,d        ; X after the switches (must still be 0x1111)
            MOVSPA
            STWA    0xF004,d        ; SP after the switches (must be userAT)
            LDWA    0xF006,d
            ADDA    1,i
            STWA    0xF006,d        ; A turns completed
            CPWA    3,i
            BRLT    aLoop
            LDWA    userAT,i
            STWA    0xF00C,d        ; where A's stack top really is
            LDWA    sysAT,i
            STWA    0xF00E,d        ; where A's system stack top really is
            LDBA    0,i
            STBA    pwrOff,d

    ; process B logs 2 and yields forever
    procB:  LDWX    logIdx,d
            LDWA    2,i
            STWA    log,x
            LDWA    logIdx,d
            ADDA    2,i
            STWA    logIdx,d
            LDWA    0x00B2,i
            LDWX    0x2222,i
            SCALL   0,i
            STWA    0xF008,d        ; B after the switches (must still be 0x00B2)
            STWX    0xF00A,d        ; X after the switches (must still be 0x2222)
            BR      procB
            .END
    """;


    @Test
    @DisplayName("two processes alternate through SCALL/SRET with registers and stacks kept separate")
    void twoProcessesAlternate() {
        State s = PepHarness.run(PROGRAM);
        assertEquals(3, mem(s, 0xF006), "A completed three turns");
        assertEquals(0x00A1, mem(s, 0xF000), "A's accumulator survived the switches");
        assertEquals(0x1111, mem(s, 0xF002), "A's index register survived the switches");
        assertEquals(0x00B2, mem(s, 0xF008), "B's accumulator survived the switches");
        assertEquals(0x2222, mem(s, 0xF00A), "B's index register survived the switches");
        assertEquals(mem(s, 0xF00C), mem(s, 0xF004), "A's user SP is back where it started");
        assertEquals(mem(s, 0xF00E), mem(s, 0xFFFB), "SRET reset the system stack top to A's own system stack");
        int[] expected = {1, 2, 1, 2, 1};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], mem(s, 0xF100 + 2 * i), "turn " + i + " of the log");
        }
    }
}
