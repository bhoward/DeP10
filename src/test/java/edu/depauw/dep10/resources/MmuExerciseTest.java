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
 * Checks the "Protection Faults" files in demo/mmu: a small kernel runs three user programs with
 * their own protection tables; two of them break the rules, and the student's onFault routine must
 * report each fault and move on to the next program.
 */
class MmuExerciseTest {
    private static final int LIMIT = 200_000;

    private static final String EXPECTED = """
            B
            Process killed: bad data access at 0x0500
            C
            Process killed: bad instruction fetch at 0x0400
            A
            All programs finished
            """;

    private static String read(String name) throws IOException {
        return Files.readString(Path.of("demo", "mmu", name));
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
    @DisplayName("Solution: both faults are reported with the right address, and A still runs")
    void solutionReportsFaults() throws IOException {
        assertEquals(EXPECTED, run(read("mmu_solution.pep"), true));
    }

    @Test
    @DisplayName("Starter: runs all three programs but does not report the faults properly")
    void starterIsIncomplete() throws IOException {
        String out = run(read("mmu_starter.pep"), true);
        assertTrue(out.contains("B\n") && out.contains("C\n") && out.contains("A\n"), out);
        assertTrue(out.endsWith("All programs finished\n"), out);
        assertFalse(out.contains("Process killed"), "the starter has no real message yet");
        assertEquals(2, out.split("\\(fault\\)", -1).length - 1, "one placeholder line per fault: " + out);
    }

    @Test
    @DisplayName("Starter and solution differ only inside the student block")
    void filesDifferOnlyInStudentBlock() throws IOException {
        String starter = read("mmu_starter.pep");
        String solution = read("mmu_solution.pep");
        String begin = "; ---- BEGIN STUDENT CODE";
        String end = "; ---- END STUDENT CODE";
        assertEquals(starter.substring(0, starter.indexOf(begin)), solution.substring(0, solution.indexOf(begin)),
                "code before the student block");
        assertEquals(starter.substring(starter.indexOf(end)), solution.substring(solution.indexOf(end)),
                "code after the student block");
    }
}
