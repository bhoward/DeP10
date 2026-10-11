package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static edu.depauw.dep10.resources.PepHarness.mem;

/**
 * Third milestone of the OS concurrency design: counting semaphores on top of
 * the cooperative scheduler of SchedulerTest.
 *
 * The trap handler dispatches on the SCALL operand: 0 = YIELD, 1 = EXIT,
 * 2 = P (wait) and 3 = V (signal); the semaphore address is passed in A and is
 * read from the saved frame at 1,s. A semaphore is one word holding its count.
 * P with a positive count decrements it and returns; with a zero count it saves
 * the caller (frame address and H, as in SchedulerTest), marks the PCB BLOCKED,
 * records the semaphore in the PCB's waitingOn field and switches to the next
 * ready process. V looks round the ring for a PCB BLOCKED on that semaphore and
 * makes it READY without touching the count (the permit is handed over, so the
 * woken process returns from its own P already holding it); with nobody waiting
 * it increments the count. The first waiter found after the caller wins, so
 * wake-up order is ring order, not first come first served.
 *
 * A PCB is 18 bytes: 0 state (0 free, 1 ready, 2 running, 3 done, 4 blocked),
 * 2 saved SP, 4 next PCB, 6 pid, 8 top of user stack, 10 top of system stack,
 * 12 rounds to run, 14 saved H, 16 waitingOn.
 *
 * Three workers each add one to a shared counter three times. Each update is
 * read, YIELD, write, which loses updates unless it sits inside P(mutex) and
 * V(mutex). Mem[F010] is the counter, Mem[F002] counts calls to the handler.
 */
class SemaphoreTest {

    private static final String PROGRAM = """
            BR      main
    FREE:   .EQUATE 0
    READY:  .EQUATE 1
    RUNNING: .EQUATE 2
    DONE:   .EQUATE 3
    BLOCKED: .EQUATE 4
    counter: .EQUATE 0xF010
    errors: .EQUATE 0xF000
    calls:  .EQUATE 0xF002
    log:    .EQUATE 0xF100
    cur:    .WORD   0               ; address of the running PCB
    mutex:  .WORD   1               ; binary semaphore: count 1
    tmpSem: .WORD   0
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
            .WORD   0
            .WORD   0
    pcb1:   .WORD   0
            .WORD   0
            .WORD   pcb2
            .WORD   1
            .WORD   user1T
            .WORD   sys1T
            .WORD   3
            .WORD   0
            .WORD   0
    pcb2:   .WORD   0
            .WORD   0
            .WORD   pcb3
            .WORD   2
            .WORD   user2T
            .WORD   sys2T
            .WORD   3
            .WORD   0
            .WORD   0
    pcb3:   .WORD   0               ; a slot that is never created
            .WORD   0
            .WORD   pcb0
            .WORD   3
            .WORD   0
            .WORD   0
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
            CPWA    2,i
            BREQ    semP
            CPWA    3,i
            BREQ    semV
            LDWA    0x0BAD,i
            STWA    0xF004,d
            BR      halt
    yield:  MOVSPA
            LDWX    cur,d
            STWA    2,x             ; save the frame address
            SWAPHA                  ; H is not in the frame
            STWA    14,x
            LDWA    READY,i
            STWA    0,x
            BR      pick
    exit:   LDWX    cur,d
            LDWA    DONE,i
            STWA    0,x
            BR      pick

    ; P: take a permit, or block the caller on the semaphore in its saved A
    semP:   LDWX    1,s
            LDWA    0,x
            BREQ    block
            SUBA    1,i
            STWA    0,x
            SRET
    block:  STWX    tmpSem,d
            MOVSPA
            LDWX    cur,d
            STWA    2,x             ; save the frame address
            SWAPHA
            STWA    14,x            ; save H
            LDWA    BLOCKED,i
            STWA    0,x
            LDWA    tmpSem,d
            STWA    16,x            ; waitingOn
            BR      pick

    ; V: hand the permit to a blocked process, or bump the count
    semV:   LDWX    1,s
            STWX    tmpSem,d
            LDWX    cur,d
            LDWX    4,x
    vLoop:  LDWA    0,x
            CPWA    BLOCKED,i
            BRNE    vNext
            LDWA    16,x
            CPWA    tmpSem,d
            BREQ    vWake
    vNext:  CPWX    cur,d
            BREQ    vNone
            LDWX    4,x
            BR      vLoop
    vWake:  LDWA    READY,i
            STWA    0,x
            SRET
    vNone:  LDWX    tmpSem,d
            LDWA    0,x
            ADDA    1,i
            STWA    0,x
            SRET

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
            LDWA    14,x
            SWAPHA                  ; restore H
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

    ; worker: 0,s = rounds left, 2,s = the counter value read inside the lock
    worker: LDWX    cur,d
            LDWA    12,x
            SUBSP   4,i
            STWA    0,s
    wLoop:  LDWA    mutex,i
            SCALL   2,i             ; P(mutex)
            LDWA    counter,d
            STWA    2,s
            SCALL   0,i             ; YIELD while holding the lock
            LDWA    2,s
            ADDA    1,i
            STWA    counter,d
            LDWA    mutex,i
            SCALL   3,i             ; V(mutex)
            LDWA    0,s
            SUBA    1,i
            STWA    0,s
            BRGT    wLoop
            SCALL   1,i
            .END
    """;

    @Test
    @DisplayName("a mutex around read, YIELD, write keeps every update: 3 workers x 3 rounds = 9")
    void mutexKeepsUpdates() {
        State s = PepHarness.run(PROGRAM);
        assertEquals(9, mem(s, 0xF010), "no update was lost");
        assertEquals(30, mem(s, 0xF002), "nine P, nine YIELD, nine V and three EXIT reached the handler");
        assertEquals(0, mem(s, 0xF004), "no unknown system call");
    }

    @Test
    @DisplayName("without the mutex the YIELD inside the update loses increments")
    void withoutMutexUpdatesAreLost() {
        String racy = PROGRAM.replace("SCALL   2,i", "SCALL   0,i").replace("SCALL   3,i", "SCALL   0,i");
        State s = PepHarness.run(racy);
        assertEquals(3, mem(s, 0xF010), "every worker read the same value in each round");
    }

    @Test
    @DisplayName("if the mutex starts at zero every worker blocks and the machine stops")
    void allBlockedStops() {
        String stuck = PROGRAM.replace("mutex:  .WORD   1", "mutex:  .WORD   0");
        State s = PepHarness.run(stuck);
        assertEquals(0, mem(s, 0xF010), "nobody entered the critical section");
        assertEquals(3, mem(s, 0xF002), "three P calls and then the scheduler found nobody ready");
    }
}
