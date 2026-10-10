package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static edu.depauw.dep10.resources.PepHarness.mem;

/**
 * Second milestone of the OS concurrency design: a cooperative round-robin
 * scheduler built on the SCALL/SRET context switch of ContextSwitchTest.
 *
 * The trap handler dispatches on the SCALL operand (read from the saved
 * instruction at 10,s): 0 = YIELD, 1 = EXIT, anything else stops the machine
 * with 0x0BAD in Mem[F004]. Processes live in a ring of PCBs linked by a next
 * field. A PCB is 14 bytes:
 *
 *   0 state (0 free, 1 ready, 2 running, 3 done)   2 saved SP (frame address)
 *   4 next PCB                                      6 pid
 *   8 top of user stack   10 top of system stack   12 rounds to run
 *
 * The create routine (A = entry point, X = PCB) builds a fake SCALL frame at
 * the top of the new process's system stack and marks it ready. The scheduler
 * (pick) walks the ring from the current PCB looking for a ready one, skips
 * free and done slots, and stops the machine when none is left. Registers live
 * only in the saved frames; there is no copy in the PCB.
 *
 * All three processes run the same worker code, keeping their pid and their
 * remaining rounds in locals on their own user stacks. Each round the worker
 * logs its pid at log[logIdx], sets A and X from its pid, yields, and checks
 * that A, X and the stack local came back intact; any mismatch adds one to the
 * error count at Mem[F000]. Mem[F002] counts calls to the handler.
 *
 * Note: a process only ever traps from user code, so its single frame always
 * sits at the top of its own system stack and the saved SP field never changes
 * value. The field is kept because a process that blocks inside a nested
 * system call (semaphores, later) will need it.
 */
class SchedulerTest {

    private static final String PROGRAM = """
            BR      main
    FREE:   .EQUATE 0
    READY:  .EQUATE 1
    RUNNING: .EQUATE 2
    DONE:   .EQUATE 3
    errors: .EQUATE 0xF000
    calls:  .EQUATE 0xF002
    log:    .EQUATE 0xF100
    cur:    .WORD   0               ; address of the running PCB
    logIdx: .WORD   0
    tmpEntry: .WORD 0
    tmpPcb: .WORD   0

    pcb0:   .WORD   0
            .WORD   0
            .WORD   pcb1
            .WORD   0
            .WORD   user0T
            .WORD   sys0T
            .WORD   3
    pcb1:   .WORD   0
            .WORD   0
            .WORD   pcb2
            .WORD   1
            .WORD   user1T
            .WORD   sys1T
            .WORD   2
    pcb2:   .WORD   0
            .WORD   0
            .WORD   pcb3
            .WORD   2
            .WORD   user2T
            .WORD   sys2T
            .WORD   1
    pcb3:   .WORD   0               ; a slot that is never created
            .WORD   0
            .WORD   pcb0
            .WORD   3
            .WORD   0
            .WORD   0
            .WORD   0

    mainS:  .BLOCK  16
    mainT:  .WORD   0
    user0:  .BLOCK  32
    user0T: .WORD   0
    user1:  .BLOCK  32
    user1T: .WORD   0
    user2:  .BLOCK  32
    user2T: .WORD   0
    sys0:   .BLOCK  64
    sys0T:  .WORD   0
    sys1:   .BLOCK  64
    sys1T:  .WORD   0
    sys2:   .BLOCK  64
    sys2T:  .WORD   0

    ; trap handler: dispatch on the SCALL operand
    handler: LDWA   calls,d
            ADDA    1,i
            STWA    calls,d
            LDWA    10,s
            BREQ    yield
            CPWA    1,i
            BREQ    exit
            LDWA    0x0BAD,i
            STWA    0xF004,d
            BR      halt
    yield:  MOVSPA
            LDWX    cur,d
            STWA    2,x             ; save the frame address
            LDWA    READY,i
            STWA    0,x
            BR      pick
    exit:   LDWX    cur,d
            LDWA    DONE,i
            STWA    0,x

    ; pick the next ready PCB after cur and resume it
    pick:   LDWX    cur,d
            LDWX    4,x
    pickLp: LDWA    0,x
            CPWA    READY,i
            BREQ    run
            CPWX    cur,d
            BREQ    halt            ; went all the way round, nobody is ready
            LDWX    4,x
            BR      pickLp
    run:    STWX    cur,d
            LDWA    RUNNING,i
            STWA    0,x
            LDWA    2,x
            MOVASP
            SRET
    halt:   LDBA    0,i
            STBA    pwrOff,d

    ; create: A = entry point, X = PCB; builds the first frame, marks ready
    create: STWA    tmpEntry,d
            STWX    tmpPcb,d
            LDWA    10,x
            SUBA    12,i
            STWA    2,x             ; saved SP = frame address
            LDWX    2,x
            LDWA    tmpEntry,d
            STWA    5,x             ; saved PC
            LDWX    tmpPcb,d
            LDWA    8,x
            LDWX    2,x
            STWA    7,x             ; saved SP = top of the user stack
            LDWX    tmpPcb,d
            LDWA    READY,i
            STWA    0,x
            RET

    main:   LDWA    mainT,i
            MOVASP
            LDWA    handler,i
            STWA    0xFFF7,d
            LDWA    worker,i
            LDWX    pcb0,i
            CALL    create
            LDWA    worker,i
            LDWX    pcb1,i
            CALL    create
            LDWA    worker,i
            LDWX    pcb2,i
            CALL    create
            LDWA    pcb3,i          ; start as if pcb3 had just exited
            STWA    cur,d
            BR      pick

    ; worker: locals on its own stack, 0,s = rounds left, 2,s = pid
    worker: LDWX    cur,d
            LDWA    6,x
            SUBSP   4,i
            STWA    2,s
            LDWA    12,x
            STWA    0,s
    wLoop:  LDWX    logIdx,d
            LDWA    2,s
            STWA    log,x
            LDWA    logIdx,d
            ADDA    2,i
            STWA    logIdx,d
            LDWA    2,s
            ADDA    0x1000,i
            LDWX    2,s
            SCALL   0,i
            SUBA    0x1000,i
            CPWA    2,s
            BRNE    wBad
            CPWX    2,s
            BREQ    wOk
    wBad:   LDWA    errors,d
            ADDA    1,i
            STWA    errors,d
    wOk:    LDWA    0,s
            SUBA    1,i
            STWA    0,s
            BRGT    wLoop
            SCALL   1,i
            .END
    """;

    @Test
    @DisplayName("three workers share one scheduler: round robin, exits are skipped, the machine stops when all are done")
    void roundRobinWithExit() {
        State s = PepHarness.run(PROGRAM);
        assertEquals(0, mem(s, 0xF000), "no register or stack local was corrupted by a switch");
        assertEquals(9, mem(s, 0xF002), "six yields and three exits reached the handler");
        assertEquals(0, mem(s, 0xF004), "no unknown system call");
        int[] expected = {0, 1, 2, 0, 1, 0};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], mem(s, 0xF100 + 2 * i), "entry " + i + " of the log");
        }
        assertEquals(0, mem(s, 0xF100 + 2 * expected.length), "nothing logged after the last exit");
    }

    @Test
    @DisplayName("an unknown SCALL code stops the machine with a marker")
    void unknownCallStops() {
        String bad = PROGRAM.replace("SCALL   1,i", "SCALL   7,i");
        State s = PepHarness.run(bad);
        assertEquals(0x0BAD, mem(s, 0xF004), "handler flagged the unknown code");
    }
}
