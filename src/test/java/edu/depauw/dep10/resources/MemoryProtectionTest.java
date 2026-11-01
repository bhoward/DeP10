package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.simulator.StepCountController;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static edu.depauw.dep10.resources.PepHarness.mem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Memory protection on the fancy machine (stage 1): a 64-byte table of two bits per 256-byte page,
 * found through the word at 0xFFF3, is enforced in user mode only. A violation traps with opcode
 * byte 0, operand 3 (read or write) or 4 (instruction fetch), the address in the word at 0xFFF1,
 * and leaves the registers and memory as they were before the instruction.
 *
 * Every program has the same layout: user code at 0 (page 0), the OS (handler, boot code, a
 * ready-made trap frame) at 0x0200, a read/write data word at 0x0400, a read-only word at 0x0500,
 * a no-access word at 0x0600, the user stack below 0x0800 and the table at 0x0C00. The handler
 * records the trap frame (A at 0xF000, opcode byte 0xF002, operand 0xF004, saved PC 0xF006, saved
 * SP 0xF008) and the fault address register (0xF00A) and powers off.
 */
class MemoryProtectionTest {
    private static final int LIMIT = 10_000;
    private static final int TABLE = 0x0C00;
    private static final int CODE = 1, READ = 2, READ_WRITE = 3, NONE = 0;

    private static final String SET_BASE = "LDWA 0x0C00,i\n        STWA 0xFFF3,d\n        ";

    private static final int[][] STANDARD_PAGES = {
            { 0x00, CODE }, { 0x04, READ_WRITE }, { 0x05, READ }, { 0x06, NONE }, { 0x07, READ_WRITE } };

    static String program(String osBefore, String user, String extraSections) {
        return """
                        BR      main
                user:   """ + user + """

                        .SECTION "os", ""
                        .ORG    0x0200
                handler: LDWA   1,s
                        STWA    0xF000,d
                        LDWA    0,i
                        LDBA    9,s
                        STWA    0xF002,d
                        LDWA    10,s
                        STWA    0xF004,d
                        LDWA    5,s
                        STWA    0xF006,d
                        LDWA    7,s
                        STWA    0xF008,d
                        LDWA    0xFFF1,d
                        STWA    0xF00A,d
                        LDBA    0,i
                        STBA    pwrOff,d
                frame:  .BYTE   0
                        .WORD   0
                        .WORD   0
                        .WORD   user
                        .WORD   0x0800
                        .BYTE   0
                        .WORD   0
                main:   LDWA    handler,i
                        STWA    0xFFF7,d
                        LDWA    0x0A00,i
                        STWA    0xFFFB,d
                """ + "        " + osBefore + """

                        LDWA    frame,i
                        MOVASP
                        SRET
                        .SECTION "rw", "rw"
                        .ORG    0x0400
                d_rw:   .WORD   0
                        .SECTION "ro", "r"
                        .ORG    0x0500
                d_ro:   .WORD   0
                        .SECTION "none", ""
                        .ORG    0x0600
                d_none: .WORD   0x6666
                """ + extraSections + "\n        .END\n";
    }

    private static void setPage(State s, int page, int bits) {
        int addr = TABLE + page / 4;
        int shift = 6 - 2 * (page % 4);
        int old = s.mem1Safe(Word.of(addr)).value();
        s.setMem1(Word.of(addr), UByte.of((old & ~(3 << shift)) | (bits << shift)));
    }

    private static State run(String osBefore, String user, String extra, int[][] pages, boolean fancy) {
        // The fancy machine clears the device registers when it starts, so the OS code turns protection on
        String enable = pages == null ? "" : SET_BASE;
        State s = PepHarness.load(PepHarness.assemble(program(enable + osBefore, user, extra)), null);
        s.setFancy(fancy);
        s.setOutput(new ByteArrayOutputStream());
        if (pages != null) {
            for (int[] p : pages) {
                setPage(s, p[0], p[1]);
            }
        }
        new Simulator(s).run(new StepCountController(new PlainController(), LIMIT));
        return s;
    }

    private static State run(String user, int[][] pages) {
        return run("", user, "", pages, true);
    }

    private static int at(State s, int addr) {
        return s.mem2Safe(Word.of(addr)).value();
    }

    private static void assertFault(State s, int operand, int address, int pc) {
        assertFalse(s.isRunning(), "the handler powered the machine off");
        assertEquals(0, mem(s, 0xF002), "opcode byte of a machine trap");
        assertEquals(operand, mem(s, 0xF004), "trap operand");
        assertEquals(address, at(s, 0xF00A), "fault address register");
        assertEquals(pc, at(s, 0xF006), "saved PC is the faulting instruction");
    }

    @Test
    @DisplayName("A store to a read-only page traps, leaves memory and A alone, and the saved PC is the store")
    void storeToReadOnlyPage() {
        State s = run("LDWA 0x1234,i\n        STWA 0x0500,d\n        SCALL 0,i\n", new int[][] { { 0, CODE }, { 4, READ_WRITE }, { 5, READ }, { 7, READ_WRITE } });
        assertFault(s, 3, 0x0500, 0x0006);
        assertEquals(0x1234, at(s, 0xF000), "A in the frame is the value before the store");
        assertEquals(0, at(s, 0x0500), "nothing was written");
        int frame = at(s, 0xFFFB) - 12;
        assertEquals(0, (at(s, frame) >> 8) & 0x80, "the fault frame says the interrupted program was in user mode (P = 0)");
    }

    @Test
    @DisplayName("A read of a no-access page traps")
    void readNoAccessPage() {
        State s = run("LDWA 0x0600,d\n        SCALL 0,i\n", STANDARD_PAGES);
        assertFault(s, 3, 0x0600, 0x0003);
        assertEquals(0, at(s, 0xF000), "A was not loaded");
    }

    @Test
    @DisplayName("A store into a code page traps (read and execute is not writable)")
    void storeToCodePage() {
        State s = run("LDWA 0x1234,i\n        STWA 0x0010,d\n        SCALL 0,i\n", STANDARD_PAGES);
        assertFault(s, 3, 0x0010, 0x0006);
    }

    @Test
    @DisplayName("Reads of read-only data and reads and writes of read/write data are allowed")
    void allowedAccesses() {
        State s = run("LDWA 0x0500,d\n        STWA 0x0400,d\n        LDWA 0x0400,d\n        STWA 0x0402,d\n        SCALL 0,i\n", STANDARD_PAGES);
        assertFalse(s.isRunning());
        assertNotEquals(0, at(s, 0xF002), "an ordinary SCALL frame, not a fault");
        assertEquals(0, at(s, 0xFFF1), "fault register untouched");
    }

    @Test
    @DisplayName("Jumping into a data page traps on the instruction fetch, operand 4")
    void fetchFromDataPage() {
        State s = run("BR 0x0400\n", STANDARD_PAGES);
        assertFault(s, 4, 0x0400, 0x0400);
    }

    @Test
    @DisplayName("An operand specifier in a page that is not executable traps, and the instruction is undone")
    void operandFetchFault() {
        String edge = """
                        .SECTION "edge", "rx"
                        .ORG    0x00FF
                        .BYTE   0xC0
                """;
        State s = run("", "LDWA 0x1111,i\n        BR 0x00FF\n", edge, STANDARD_PAGES, true);
        assertFault(s, 4, 0x0100, 0x00FF);
        assertEquals(0x1111, at(s, 0xF000), "A still holds the earlier value");
    }

    @Test
    @DisplayName("Supervisor mode ignores the table: the OS can write read-only and no-access pages")
    void supervisorIsNotChecked() {
        State s = run("LDWA 0x5555,i\n        STWA 0x0500,d\n        STWA 0x0600,d\n        LDWA 0x0600,d\n        STWA 0xF00C,d\n",
                "SCALL 0,i\n", "", STANDARD_PAGES, true);
        assertEquals(0x5555, at(s, 0x0500));
        assertEquals(0x5555, at(s, 0x0600));
        assertEquals(0x5555, at(s, 0xF00C));
        assertNotEquals(0, at(s, 0xF002), "the user's SCALL is an ordinary trap");
    }

    @Test
    @DisplayName("With the table base register at 0 nothing is protected")
    void protectionOffByDefault() {
        State s = run("", "LDWA 0x1234,i\n        STWA 0x0500,d\n        STWA 0x0600,d\n        SCALL 0,i\n", "", null, true);
        assertEquals(0x1234, at(s, 0x0500));
        assertEquals(0x1234, at(s, 0x0600));
        assertNotEquals(0, at(s, 0xF002));
    }

    @Test
    @DisplayName("The plain machine ignores the table")
    void plainMachineIgnoresTable() {
        State s = run("", "LDWA 0x1234,i\n        STWA 0x0500,d\n        SCALL 0,i\n", "", STANDARD_PAGES, false);
        assertEquals(0x1234, at(s, 0x0500));
    }

    @Test
    @DisplayName("The I/O page 0xFF is closed to user mode even if the table opens it")
    void ioPageAlwaysClosed() {
        int[][] open = { { 0, CODE }, { 7, READ_WRITE }, { 0xFF, READ_WRITE } };
        State s = run("LDWA 0x0041,i\n        STBA 0xFFFE,d\n        SCALL 0,i\n", open);
        assertEquals(3, mem(s, 0xF004));
        assertEquals(0xFFFE, at(s, 0xF00A));
    }

    @Test
    @DisplayName("A word store that crosses into a read-only page writes neither byte")
    void wordAcrossPages() {
        State s = run("LDWA 0x1234,i\n        STWA 0x04FF,d\n        SCALL 0,i\n", STANDARD_PAGES);
        assertFault(s, 3, 0x0500, 0x0006);
        assertEquals(0, s.mem1Safe(Word.of(0x04FF)).value(), "the first byte was not written either");
    }

    @Test
    @DisplayName("CALL into a read-only stack faults after its SP change, which is undone")
    void callUndone() {
        State s = run("LDWA 0x0580,i\n        MOVASP\n        CALL user\n", STANDARD_PAGES);
        assertEquals(3, mem(s, 0xF004));
        assertEquals(0x057E, at(s, 0xF00A), "return address slot");
        assertEquals(0x0580, at(s, 0xF008), "SP in the frame is the value before CALL");
    }

    @Test
    @DisplayName("Flags on .SECTION give the protection table: x is code, w is read/write, r is read only")
    void sectionFlagsBuildTable() {
        var result = PepHarness.assembleResult(program("", "SCALL 0,i\n", """
                        .SECTION "code", "rx"
                        .ORG    0x0800
                        .WORD   1
                        .SECTION "both", "rwx"
                        .ORG    0x0900
                        .WORD   1
                        .SECTION "z", "rwz"
                        .ORG    0x0A00
                        .WORD   1
                """));
        byte[] t = result.protectionTable();
        assertEquals(64, t.length);
        assertEquals(READ_WRITE, (t[4 / 4] >> 6) & 3, "page 4 holds the rw section");
        assertEquals(READ, (t[5 / 4] >> 4) & 3, "page 5 is read only");
        assertEquals(0, (t[6 / 4] >> 2) & 3, "page 6 has empty flags");
        assertEquals(CODE, (t[8 / 4] >> 6) & 3, "rx is a code page");
        assertEquals(CODE, (t[9 / 4] >> 4) & 3, "rwx cannot be writable and executable, so it is code");
        assertEquals(READ_WRITE, (t[10 / 4] >> 2) & 3, "z does not matter");
        assertEquals(0, (t[3 / 4] >> 0) & 3, "page 3 belongs to no flagged section");
    }

    @Test
    @DisplayName("A table built from the section flags protects a running program")
    void tableFromFlagsRuns() {
        String user = "LDWA 0x1234,i\n        STWA 0x0500,d\n        SCALL 0,i\n";
        var result = PepHarness.assembleResult(program(SET_BASE, user, ""));
        State s = PepHarness.load(result.toObjectFile(), null);
        s.setFancy(true);
        s.setOutput(new ByteArrayOutputStream());
        byte[] table = result.protectionTable();
        for (int i = 0; i < table.length; i++) {
            s.setMem1(Word.of(TABLE + i), UByte.of(table[i] & 0xFF));
        }
        setPage(s, 0, CODE); // the first section has no .SECTION directive, so it carries no flags
        setPage(s, 7, READ_WRITE);
        new Simulator(s).run(new StepCountController(new PlainController(), LIMIT));
        assertFault(s, 3, 0x0500, 0x0006);
    }
}
