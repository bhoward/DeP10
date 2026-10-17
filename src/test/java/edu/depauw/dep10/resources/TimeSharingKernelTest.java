package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.State;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static edu.depauw.dep10.resources.PepHarness.mem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the files for the "Taking Turns on One CPU" exercise in demo/kernel:
 * the starter kernel students receive and the instructor solution. The two
 * files differ only in the block between the STUDENT CODE markers.
 */
class TimeSharingKernelTest {
    private static final int LIMIT = 100_000;

    private static String read(String name) throws IOException {
        return Files.readString(Path.of("demo", "kernel", name));
    }

    @Test
    @DisplayName("Starter as given: no switching, each program runs to completion in turn")
    void starterRunsWithoutSwitching() throws IOException {
        String out = PepHarness.runForOutput(read("timeshare_starter.pep"), LIMIT);
        assertEquals("A1\nA2\nA3\nB1\nB2\nB3\n", out);
    }

    @Test
    @DisplayName("Solution: output alternates and H survives every switch")
    void solutionAlternates() throws IOException {
        String out = PepHarness.runForOutput(read("timeshare_solution.pep"), LIMIT);
        assertEquals("A1\nB1\nA2\nB2\nA3\nB3\n", out);
    }

    @Test
    @DisplayName("Solution: when B finishes first, A keeps running instead of switching to a finished process")
    void finishedProcessIsSkipped() throws IOException {
        String src = read("timeshare_solution.pep").replace("bEnd:     .EQUATE 4", "bEnd:     .EQUATE 2");
        assertFalse(src.equals(read("timeshare_solution.pep")), "the edit must change the program");
        String out = PepHarness.runForOutput(src, LIMIT);
        assertEquals("A1\nB1\nA2\nA3\n", out);
    }

    @Test
    @DisplayName("Not saving H makes the user programs report a lost H")
    void lostHIsDetected() throws IOException {
        String src = read("timeshare_solution.pep").replace("STWA    stH,x", "NOP");
        assertFalse(src.equals(read("timeshare_solution.pep")), "the edit must change the program");
        String out = PepHarness.runForOutput(src, LIMIT);
        assertTrue(out.contains("!"), "output was: " + out);
    }

    @Test
    @DisplayName("An unknown SCALL code stops the machine with a marker")
    void unknownCallStops() throws IOException {
        String src = read("timeshare_solution.pep").replaceFirst("SCALL   0,i           ; YIELD", "SCALL   7,i");
        State s = PepHarness.run(src, LIMIT);
        assertEquals(0x0BAD, mem(s, 0xF004));
    }

    @Test
    @DisplayName("Starter and solution differ only inside the student block")
    void filesDifferOnlyInStudentBlock() throws IOException {
        String starter = read("timeshare_starter.pep");
        String solution = read("timeshare_solution.pep");
        String begin = "; ---- BEGIN STUDENT CODE";
        String end = "; ---- END STUDENT CODE";
        String sTail = starter.substring(starter.indexOf(end));
        String oTail = solution.substring(solution.indexOf(end));
        assertEquals(sTail, oTail, "code after the student block");
        String sHead = starter.substring(starter.indexOf("        BR      main"), starter.indexOf(begin));
        String oHead = solution.substring(solution.indexOf("        BR      main"), solution.indexOf(begin));
        assertEquals(sHead, oHead, "code before the student block");
    }
}
