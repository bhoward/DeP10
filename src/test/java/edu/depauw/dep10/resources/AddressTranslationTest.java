package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.simulator.StepCountController;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static edu.depauw.dep10.resources.PepHarness.mem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Address translation on the fancy machine (MMU stage 2). An odd value in the base register at 0xFFF3
 * selects a page table of 256 words at the even address (value minus 1): the high byte of each word is the
 * physical frame number and the low two bits are the same protection bits as stage 1
 * (00 not present, 10 read, 11 read/write, 01 read and execute). Supervisor mode is not translated.
 *
 * The programs use the layout of MemoryProtectionTest: user code in page 0, the user stack in page 7, the table
 * at 0x0C00 (so the base register holds 0x0C01).
 */
class AddressTranslationTest {
    private static final int LIMIT = 10_000;
    private static final int CODE = 1, READ = 2, READ_WRITE = 3, NONE = 0;
    private static final String SET_BASE = "LDWA 0x0C01,i\n        STWA 0xFFF3,d\n        ";

    private static void map(State s, int page, int frame, int bits) {
        s.setMem1(Word.of(0x0C00 + 2 * page), UByte.of(frame));
        s.setMem1(Word.of(0x0C00 + 2 * page + 1), UByte.of(bits));
    }

    private static State run(String user) {
        return run("", user, true, new int[0][]);
    }

    /** osBefore runs in supervisor mode before the SRET; extra rows are { page, frame, bits }. */
    private static State run(String osBefore, String user, boolean stackMapped, int[][] extra) {
        State s = PepHarness.load(PepHarness.assemble(MemoryProtectionTest.program(SET_BASE + osBefore, user, "")), null);
        s.setFancy(true);
        s.setOutput(new ByteArrayOutputStream());
        map(s, 0, 0x00, CODE);
        map(s, 4, 0x10, READ_WRITE);   // virtual 0x04xx is physical 0x10xx
        map(s, 5, 0x11, READ);         // virtual 0x05xx is physical 0x11xx, read only
        map(s, 6, 0x00, NONE);         // not present
        if (stackMapped) {
            map(s, 7, 0x07, READ_WRITE);   // the user stack
        }
        for (int[] e : extra) {
            map(s, e[0], e[1], e[2]);
        }
        new Simulator(s).run(new StepCountController(new PlainController(), LIMIT));
        return s;
    }

    private static int at(State s, int addr) {
        return s.mem2Safe(Word.of(addr)).value();
    }

    @Test
    @DisplayName("A user store to a virtual page lands in the mapped frame, and reads see it")
    void storeGoesToFrame() {
        State s = run("LDWA 0x1234,i\n        STWA 0x0400,d\n        LDWA 0x0400,d\n        STWA 0x0402,d\n        SCALL 0,i\n");
        assertEquals(0x1234, at(s, 0x1000), "physical frame 0x10");
        assertEquals(0x1234, at(s, 0x1002), "a read through the same mapping");
        assertEquals(0, at(s, 0x0400), "the physical page with the same number is untouched");
    }

    @Test
    @DisplayName("A word that crosses a page boundary uses each page's own frame")
    void wordAcrossMappedPages() {
        State s = run("LDWA 0x1234,i\n        STBA 0x04FF,d\n        STBA 0x0500,d\n        SCALL 0,i\n");
        // The second store hits a read-only page and traps; only the first byte was written
        assertEquals(0x34, s.mem1Safe(Word.of(0x10FF)).value());
        assertEquals(0, s.mem1Safe(Word.of(0x1100)).value());
        assertEquals(3, mem(s, 0xF004));
        assertEquals(0x0500, at(s, 0xF00A), "the fault address is the virtual address");
    }

    @Test
    @DisplayName("A page that is not present traps with the virtual address")
    void notPresentTraps() {
        State s = run("LDWA 0x0600,d\n        SCALL 0,i\n");
        assertFalse(s.isRunning());
        assertEquals(0, mem(s, 0xF002));
        assertEquals(3, mem(s, 0xF004));
        assertEquals(0x0600, at(s, 0xF00A));
    }

    @Test
    @DisplayName("Supervisor mode is not translated: the OS sees physical memory")
    void supervisorIsPhysical() {
        State s = run("SCALL 0,i\n");
        assertEquals(0x0C01, at(s, 0xFFF3), "the base register is still set");
        assertEquals(0, at(s, 0x1000));
    }

    @Test
    @DisplayName("Two page tables map the same virtual page to different frames")
    void twoAddressSpaces() {
        // The second table, at 0x0E00, maps virtual page 4 to frame 0x12; the first maps it to 0x10
        State s = PepHarness.load(PepHarness.assemble(MemoryProtectionTest.program(SET_BASE, "LDWA 0x4321,i\n        STWA 0x0400,d\n        SCALL 0,i\n", "")), null);
        s.setFancy(true);
        s.setOutput(new ByteArrayOutputStream());
        for (int t : new int[] { 0x0C00, 0x0E00 }) {
            int[][] entries = { { 0, 0x00, CODE }, { 4, t == 0x0C00 ? 0x10 : 0x12, READ_WRITE }, { 7, 0x07, READ_WRITE } };
            for (int[] e : entries) {
                s.setMem1(Word.of(t + 2 * e[0]), UByte.of(e[1]));
                s.setMem1(Word.of(t + 2 * e[0] + 1), UByte.of(e[2]));
            }
        }
        new Simulator(s).run(new StepCountController(new PlainController(), LIMIT));
        assertEquals(0x4321, at(s, 0x1000), "the first process wrote to frame 0x10");
        assertEquals(0, at(s, 0x1200), "frame 0x12 belongs to the other address space");
    }

    @Test
    @DisplayName("Page 0xFF stays closed to user mode even when the table maps it read/write")
    void ioPageClosedEvenIfMapped() {
        State s = run("", "LDWA 0x0041,i\n        STBA 0xFFFE,d\n        SCALL 0,i\n", true, new int[][] { { 0xFF, 0xFF, READ_WRITE } });
        assertEquals(3, mem(s, 0xF004));
        assertEquals(0xFFFE, at(s, 0xF00A));
    }

    @Test
    @DisplayName("User mode cannot change the base register: a store to 0xFFF3 faults and the base is unchanged")
    void baseRegisterIsOutOfReach() {
        State s = run("", "LDWA 0,i\n        STWA 0xFFF3,d\n        SCALL 0,i\n", true, new int[0][]);
        assertEquals(3, mem(s, 0xF004));
        assertEquals(0xFFF3, at(s, 0xF00A));
        assertEquals(0x0C01, at(s, 0xFFF3));
    }

    @Test
    @DisplayName("A word store whose second byte is in a page that is not present writes neither byte")
    void wordAcrossNotPresentPage() {
        State s = run("", "LDWA 0x1234,i\n        STWA 0x04FF,d\n        SCALL 0,i\n", true, new int[][] { { 5, 0x11, NONE } });
        assertEquals(3, mem(s, 0xF004));
        assertEquals(0x0500, at(s, 0xF00A));
        assertEquals(0, s.mem1Safe(Word.of(0x10FF)).value(), "the first byte was not written either");
    }

    @Test
    @DisplayName("The timer still fires under translation, and the saved PC is a virtual address")
    void timerWorksUnderTranslation() {
        State s = run("LDWA 30,i\n        STWA 0xFFF5,d\n", "LDWA 0,i\nspin: ADDA 1,i\n        BR spin\n", true, new int[0][]);
        assertFalse(s.isRunning());
        assertEquals(0, mem(s, 0xF002));
        assertEquals(1, mem(s, 0xF004), "timer trap");
        int pc = at(s, 0xF006);
        assertTrue(pc < 0x100, "the saved PC is in virtual page 0: " + pc);
    }

    @Test
    @DisplayName("A system call from user mode works even when the user stack page is not mapped")
    void scallNeedsNoUserStack() {
        State s = run("", "SCALL 0,i\n", false, new int[0][]);
        assertFalse(s.isRunning());
        assertNotEquals(0, mem(s, 0xF002), "an ordinary system call frame, not a fault");
    }
}
