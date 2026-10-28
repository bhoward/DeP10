package edu.depauw.dep10.resources;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.op.Pep10;
import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.simulator.StepCountController;
import edu.depauw.dep10.util.UByte;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static edu.depauw.dep10.resources.PepHarness.mem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the "fancy machine" switch: protected (supervisor) versus user mode, the timer at
 * 0xFFF5 and the invalid-instruction trap. The same programs are also run with the switch off
 * to check that the original machine is unchanged.
 *
 * Every program has the same shape: main installs the trap handler, points the system stack
 * at the top of a ready-made trap frame, and SRETs into the user code. The handler records
 * what it finds in the frame (A at 0xF000, opcode byte at 0xF002, operand at 0xF004, saved PC
 * at 0xF006) and powers off.
 */
class FancyMachineTest {
    private static final int LIMIT = 10_000;

    private static String program(String beforeSret, String user) {
        return """
                        BR      main
                sysS:   .BLOCK  64
                frame:  .BYTE   0
                        .WORD   0
                        .WORD   0
                        .WORD   user
                        .WORD   userT
                        .BYTE   0
                        .WORD   0
                userS:  .BLOCK  32
                userT:  .WORD   0
                handler: LDWA   1,s
                        STWA    0xF000,d
                        LDWA    0,i
                        LDBA    9,s
                        STWA    0xF002,d
                        LDWA    10,s
                        STWA    0xF004,d
                        LDWA    5,s
                        STWA    0xF006,d
                        LDBA    0,i
                        STBA    pwrOff,d
                main:   LDWA    handler,i
                        STWA    0xFFF7,d
                        LDWA    userS,i
                        STWA    0xFFFB,d
                """ + "        " + beforeSret + """
                        LDWA    frame,i
                        MOVASP
                        SRET
                user:   """ + user + "\n        .END\n";
    }

    private static State run(String src, boolean fancy, int limit, ByteArrayOutputStream err) {
        State s = PepHarness.load(PepHarness.assemble(src), null);
        s.setFancy(fancy);
        if (err != null) {
            s.setError(err);
        }
        new Simulator(s).run(new StepCountController(new PlainController(), limit));
        return s;
    }

    private static final String COUNT_LOOP = """
                    LDWA    0,i
            u:      ADDA    1,i
                    BR      u
            """;

    @Test
    @DisplayName("Timer: counts every instruction and traps with opcode 0, operand 1")
    void timerTrapsAfterCountedInstructions() {
        State s = run(program("LDWA 10,i\n        STWA 0xFFF5,d\n", COUNT_LOOP), true, LIMIT, null);
        assertFalse(s.isRunning());
        assertEquals(3, mem(s, 0xF000), "10 ticks = LDWA, MOVASP, SRET in supervisor mode, then 7 user instructions: LDWA, ADDA, BR, ADDA, BR, ADDA, BR");
        assertEquals(0, mem(s, 0xF002), "opcode byte of a timer trap");
        assertEquals(1, mem(s, 0xF004), "operand of a timer trap");
        assertEquals(0, mem(s, 0xFFF5), "timer is back at 0");
        assertTrue(s.isProtectedMode(), "the handler runs in protected mode");
    }

    @Test
    @DisplayName("Timer: supervisor-mode instructions count too; a timer that expires there traps after one user instruction")
    void supervisorTicksAndTrapIsDeferred() {
        String nops = "LDWA 3,i\n        STWA 0xFFF5,d\n" + "        NOP\n".repeat(20);
        State s = run(program(nops, COUNT_LOOP), true, LIMIT, null);
        assertFalse(s.isRunning());
        assertEquals(0, mem(s, 0xF000), "the timer ran out inside the OS code, so the trap came right after the first user instruction (LDWA)");
        assertTrue(mem(s, 0xF006) > 0, "a frame was pushed");
        assertEquals(1, mem(s, 0xF004));
    }

    @Test
    @DisplayName("Timer set to 0 means no timer: the user program runs until its own SCALL")
    void zeroTimerNeverFires() {
        String user = """
                    LDWA    0,i
                u:  ADDA    1,i
                    CPWA    100,i
                    BRLT    u
                    SCALL   0,i
                """;
        State s = run(program("", user), true, LIMIT, null);
        assertFalse(s.isRunning());
        assertEquals(100, mem(s, 0xF000));
        assertNotEquals(0, mem(s, 0xF002), "a SCALL frame carries the SCALL opcode");
        assertEquals(0, mem(s, 0xF004), "operand of SCALL 0");
    }

    @Test
    @DisplayName("Without the fancy switch the timer is ignored")
    void timerIgnoredOnPlainMachine() {
        State s = run(program("LDWA 10,i\n        STWA 0xFFF5,d\n", COUNT_LOOP), false, 500, null);
        assertTrue(s.isRunning() || mem(s, 0xF004) == 0, "no timer trap happened");
        assertEquals(10, mem(s, 0xFFF5), "timer word is just memory");
    }

    /** A real opcode byte that is not an instruction and not a prefix. */
    private static int badOpcode() {
        for (int n = 1; n < 256; n++) {
            if (Pep10.table.getOp(UByte.of(n)) == Operation.UNIMPLEMENTED) {
                return n;
            }
        }
        throw new AssertionError("every opcode is in use");
    }

    @Test
    @DisplayName("Invalid instruction in user mode traps to the handler with operand 2")
    void invalidInstructionInUserModeTraps() {
        int bad = badOpcode();
        State s = run(program("", String.format("    .BYTE   %d\n", bad)), true, LIMIT, null);
        assertFalse(s.isRunning());
        assertEquals(bad, mem(s, 0xF002), "the offending opcode is in the frame");
        assertEquals(2, mem(s, 0xF004));
    }

    @Test
    @DisplayName("Opcode 0 is invalid too, and its frame is told apart from a timer trap by the operand")
    void zeroOpcodeIsInvalid() {
        State s = run(program("", "    .BYTE   0\n"), true, LIMIT, null);
        assertEquals(0, mem(s, 0xF002));
        assertEquals(2, mem(s, 0xF004));
    }

    @Test
    @DisplayName("Invalid instruction in supervisor mode stops the machine with a message")
    void invalidInstructionInSupervisorModeStops() {
        var err = new ByteArrayOutputStream();
        State s = run(program("        .BYTE   " + badOpcode() + "\n", COUNT_LOOP), true, LIMIT, err);
        assertFalse(s.isRunning());
        assertEquals(0, mem(s, 0xF002), "the handler never ran");
        assertTrue(err.toString(StandardCharsets.US_ASCII).contains("Invalid instruction"));
    }

    @Test
    @DisplayName("Without the fancy switch an invalid instruction is still an error, as before")
    void invalidInstructionOnPlainMachine() {
        assertThrows(IllegalStateException.class,
                () -> run(program("", String.format("    .BYTE   %d\n", badOpcode())), false, LIMIT, null));
    }

    @Test
    @DisplayName("SRET leaves protected mode")
    void sretLeavesProtectedMode() {
        State s = PepHarness.load(PepHarness.assemble(program("", COUNT_LOOP)), null);
        s.setFancy(true);
        assertTrue(s.isProtectedMode(), "the machine boots in the OS");
        // run until the first user instruction has executed: BR, 4 setup pairs, ... just step
        var sim = new Simulator(s);
        boolean sawUser = false;
        for (int i = 0; i < 200 && s.isRunning(); i++) {
            s.doStep(new PlainController(), Pep10.table);
            if (!s.isProtectedMode()) {
                sawUser = true;
                break;
            }
        }
        assertTrue(sawUser, "protected mode was left by SRET");
    }

    @Test
    @DisplayName("P flag: MOVFLGA shows bit 7 = 1 in protected mode and 0 in user mode")
    void pFlagFollowsMode() {
        State s = run(program("MOVFLGA\n        STWA 0xF010,d\n", "MOVFLGA\n        STWA 0xF012,d\n        SCALL 0,i\n"), true, LIMIT, null);
        assertFalse(s.isRunning());
        assertEquals(0x80, mem(s, 0xF010) & 0x80, "protected mode: P set");
        assertEquals(0, mem(s, 0xF012) & 0x80, "user mode: P clear");
    }

    @Test
    @DisplayName("P flag: MOVAFLG cannot set it in user mode or clear it in protected mode")
    void pFlagCannotBeWritten() {
        String os = "LDWA 0,i\n        MOVAFLG\n        MOVFLGA\n        STWA 0xF010,d\n";
        String user = "LDWA 0xFF,i\n        MOVAFLG\n        MOVFLGA\n        STWA 0xF012,d\n        SCALL 0,i\n";
        State s = run(program(os, user), true, LIMIT, null);
        assertFalse(s.isRunning());
        assertEquals(0x80, mem(s, 0xF010) & 0x80, "writing 0 in protected mode leaves P set");
        assertEquals(0, mem(s, 0xF012) & 0x80, "writing 0xFF in user mode leaves P clear");
        assertEquals(0x0F, mem(s, 0xF012) & 0xFF, "the NZVC bits do change");
    }

    @Test
    @DisplayName("P flag: the trap frame records the interrupted mode (user: P = 0)")
    void frameHoldsInterruptedP() {
        State s = run(program("", "LDWA 0,i\n        SCALL 0,i\n"), true, LIMIT, null);
        int frame = mem(s, 0xFFFB) - 12;
        assertEquals(mem(s, 0xF006), mem(s, frame + 5), "found the frame: its saved PC matches what the handler read");
        assertEquals(0, (mem(s, frame) >> 8) & 0x80, "flags byte of the frame pushed from user mode");
    }

    @Test
    @DisplayName("P flag: always 0 on the plain machine")
    void pFlagOffOnPlainMachine() {
        State s = run(program("MOVFLGA\n        STWA 0xF010,d\n", "SCALL 0,i\n"), false, 200, null);
        assertEquals(0, mem(s, 0xF010) & 0x80);
    }

    @Test
    @DisplayName("SRET returns to the mode recorded in the frame: P = 1 in the frame means supervisor")
    void sretRestoresModeFromFrame() {
        // the OS marks the ready-made frame as coming from supervisor mode before it SRETs
        State s = run(program("LDBA 0x80,i\n        STBA frame,d\n",
                "MOVFLGA\n        STWA 0xF010,d\n        SCALL 0,i\n"), true, LIMIT, null);
        assertFalse(s.isRunning());
        assertEquals(0x80, mem(s, 0xF010) & 0x80, "still in supervisor mode after SRET");
    }

    @Test
    @DisplayName("SRET is privileged: in user mode it traps like an invalid instruction (operand 2)")
    void sretInUserModeTraps() {
        State s = run(program("", "SRET\n"), true, LIMIT, null);
        assertFalse(s.isRunning());
        assertEquals(2, mem(s, 0xF002), "the SRET opcode is in the frame");
        assertEquals(2, mem(s, 0xF004));
    }

    @Test
    @DisplayName("BRP branches when P is set (supervisor) and falls through in user mode")
    void brpFollowsP() {
        String os = "BRP sup,i\n        LDWA 1,i\n        STWA 0xF010,d\n        BR cont\n"
                + "sup:    LDWA 2,i\n        STWA 0xF010,d\n"
                + "cont:   NOP\n";
        String user = "BRP usup,i\n        LDWA 1,i\n        STWA 0xF012,d\n        SCALL 0,i\n"
                + "usup:   LDWA 2,i\n        STWA 0xF012,d\n        SCALL 0,i\n";
        State s = run(program(os, user), true, LIMIT, null);
        assertFalse(s.isRunning());
        assertEquals(2, mem(s, 0xF010), "taken in supervisor mode");
        assertEquals(1, mem(s, 0xF012), "not taken in user mode");
    }

    @Test
    @DisplayName("BRP is never taken on the plain machine")
    void brpNeverTakenOnPlainMachine() {
        State s = run(program("", "BRP usup,i\n        LDWA 1,i\n        STWA 0xF012,d\n        SCALL 0,i\n"
                + "usup:   LDWA 2,i\n        STWA 0xF012,d\n        SCALL 0,i\n"), false, 200, null);
        assertEquals(1, mem(s, 0xF012));
    }
}
