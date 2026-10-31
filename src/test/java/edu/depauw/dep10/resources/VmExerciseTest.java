package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.simulator.StepCountController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the "Page Faults" files in demo/vm: a small kernel runs a user program with address
 * translation on; the student's onFault routine must map a frame for each page fault (demand paging)
 * and kill the process when it touches a page that is not its own.
 */
class VmExerciseTest {
    private static final int LIMIT = 200_000;

    private static final String EXPECTED = """
            Page fault at 0x0400, frame 0x0020
            Page fault at 0x0500, frame 0x0021
            Page fault at 0x0600, frame 0x0022
            6
            Process killed: bad access at 0x0900
            All programs finished
            """;

    private static String read(String name) throws IOException {
        return Files.readString(Path.of("demo", "vm", name));
    }

    private static String run(String src, boolean fancy) {
        State s = PepHarness.load(PepHarness.assemble(src), null);
        s.setFancy(fancy);
        var out = new ByteArrayOutputStream();
        s.setOutput(out);
        var err = new ByteArrayOutputStream();
        s.setError(err);
        new Simulator(s).run(new StepCountController(new PlainController(), LIMIT));
        assertFalse(s.isRunning(), "machine should have powered off");
        return out.toString(StandardCharsets.US_ASCII);
    }

    @Test
    @DisplayName("Solution: three pages are mapped on demand, the sum is right, and the stray access kills the process")
    void solutionPagesIn() throws IOException {
        assertEquals(EXPECTED, run(read("vm_solution.pep"), true));
    }

    @Test
    @DisplayName("Solution: the data really lives in the new frames")
    void dataIsInFrames() throws IOException {
        State s = PepHarness.load(PepHarness.assemble(read("vm_solution.pep")), null);
        s.setFancy(true);
        s.setOutput(new ByteArrayOutputStream());
        new Simulator(s).run(new StepCountController(new PlainController(), LIMIT));
        assertEquals(1, s.mem2Safe(edu.depauw.dep10.util.Word.of(0x2000)).value());
        assertEquals(2, s.mem2Safe(edu.depauw.dep10.util.Word.of(0x2100)).value());
        assertEquals(3, s.mem2Safe(edu.depauw.dep10.util.Word.of(0x2200)).value());
        assertEquals(0, s.mem2Safe(edu.depauw.dep10.util.Word.of(0x0400)).value(), "virtual page 4 is not physical page 4");
    }

    @Test
    @DisplayName("Starter: the first fault ends the program with a placeholder message")
    void starterIsIncomplete() throws IOException {
        String out = run(read("vm_starter.pep"), true);
        assertEquals("(fault)\nAll programs finished\n", out);
    }

    @Test
    @DisplayName("Starter and solution differ only inside the student block")
    void filesDifferOnlyInStudentBlock() throws IOException {
        String starter = read("vm_starter.pep");
        String solution = read("vm_solution.pep");
        String begin = "; ---- BEGIN STUDENT CODE";
        String end = "; ---- END STUDENT CODE";
        assertEquals(starter.substring(0, starter.indexOf(begin)), solution.substring(0, solution.indexOf(begin)),
                "code before the student block");
        assertEquals(starter.substring(starter.indexOf(end)), solution.substring(solution.indexOf(end)),
                "code after the student block");
    }
}
