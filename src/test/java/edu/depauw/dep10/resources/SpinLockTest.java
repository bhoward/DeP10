package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static edu.depauw.dep10.resources.PepHarness.mem;

/**
 * A spin lock built on SWAPA (the atomic exchange of A with a memory word).
 * SWAPA sets Z from the OLD value of the memory word, so a branch right after
 * it tells whether the lock was free: Z set means the old value was 0 (free),
 * Z clear means someone else holds it.
 *
 * The simulator cannot interleave two processes yet, so this plays two
 * "processes" one after the other against one shared lock word. It checks
 * the lock logic, not true preemption.
 */
class SpinLockTest {

    private static State run(String body) {
        return PepHarness.run("        BR      main\nlock:   .WORD   0\nmain:   NOP\n" + body + "\n        .END\n");
    }

    @Test
    @DisplayName("SWAPA lock: a free lock is acquired, a held lock is refused until released")
    void freeThenHeldThenReleased() {
        State s = run("""
                ; process A acquires the free lock
                LDWA    1,i
                SWAPA   lock,d
                STWA    0xF000,d        ; old value A saw (0 = was free)
                ; process B tries three times while A holds it
                LDWX    0,i             ; failed attempts
        bTry:   LDWA    1,i
                SWAPA   lock,d
                BREQ    bGot            ; Z set: old value was 0, B got the lock
                ADDX    1,i
                CPWX    3,i
                BRLT    bTry
                STWX    0xF002,d        ; B gave up after three failures
                ; process A releases
                LDWA    0,i
                STWA    lock,d
                ; process B tries again
                LDWA    1,i
                SWAPA   lock,d
                STWA    0xF004,d        ; old value B saw now (0 = was free)
                LDWA    lock,d
                STWA    0xF006,d        ; lock is held again, by B
                BR      done
        bGot:   LDWA    0xBAD,i         ; B must not get the lock while A holds it
                STWA    0xF002,d
        done:   LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0, mem(s, 0xF000), "A found the lock free");
        assertEquals(3, mem(s, 0xF002), "B failed all three attempts while A held the lock");
        assertEquals(0, mem(s, 0xF004), "B found the lock free after A released it");
        assertEquals(1, mem(s, 0xF006), "lock is held by B");
    }

    @Test
    @DisplayName("SWAPA exchanges A and memory and sets Z and N from the old memory value")
    void swapSetsFlagsFromOldValue() {
        State s = run("""
                LDWA    0x8000,i
                STWA    lock,d
                LDWA    5,i
                SWAPA   lock,d          ; A = 0x8000, lock = 5, N set from the old value
                BRLT    neg
                LDWA    0,i
                STWA    0xF000,d
                BR      next
        neg:    LDWA    1,i
                STWA    0xF000,d
        next:   LDWA    lock,d
                STWA    0xF002,d
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(1, mem(s, 0xF000), "N was set from the old value 0x8000");
        assertEquals(5, mem(s, 0xF002), "memory now holds the old A");
    }
}
