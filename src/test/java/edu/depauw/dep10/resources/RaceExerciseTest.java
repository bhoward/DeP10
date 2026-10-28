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
 * Checks the "Two Threads, One Printer" files in demo/race: the starter, whose printLong keeps its
 * working variables in static storage, and the solution, which keeps them on the stack. The
 * program runs two threads under the fancy machine's timer. Each thread's characters go into its
 * own buffer, and when both have finished the kernel prints each buffer and a verdict (OK or
 * WRONG) comparing it with what that thread should have printed.
 */
class RaceExerciseTest {
    private static final String A_TEXT = "2147483647\n-2147483648\n0\n1234567\n-90\n";
    private static final String B_TEXT = "100000\n-1\n2000000000\n7\n-305419896\n";
    private static final int LIMIT = 20_000_000;
    private static final String QUANTUM = "quantum:  .EQUATE 40";

    private static String read(String name) throws IOException {
        return Files.readString(Path.of("demo", "race", name));
    }

    private static final String REPORT_A = "Thread 1 wrote:\n" + A_TEXT + "Thread 1: OK\n";
    private static final String REPORT_B = "Thread 2 wrote:\n" + B_TEXT + "Thread 2: OK\n";

    /** Runs the program on the fancy machine with the given quantum; returns the report the kernel prints at the end. */
    private static String run(String src, int quantum) {
        String text = src.replace(QUANTUM, "quantum:  .EQUATE " + quantum);
        assertFalse(text.equals(src) && quantum != 40, "the quantum edit must change the program");
        State s = PepHarness.load(PepHarness.assemble(text), null);
        s.setFancy(true);
        var out = new ByteArrayOutputStream();
        s.setOutput(out);
        new Simulator(s).run(new StepCountController(new PlainController(), LIMIT));
        assertFalse(s.isRunning(), "machine should have powered off (quantum " + quantum + ")");
        assertEquals(0, PepHarness.mem(s, 0xF004), "no unknown system call (quantum " + quantum + ")");
        return out.toString(StandardCharsets.US_ASCII);
    }

    private static boolean correct(String out) {
        return out.contains("Thread 1: OK") && out.contains("Thread 2: OK");
    }

    private static final int[] QUANTA = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 17, 19, 23, 29, 31, 37, 40, 41, 43,
            47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 127, 211, 509, 997, 4001};

    @Test
    @DisplayName("Solution: every quantum gives each thread its own correct output")
    void solutionIsSafe() throws IOException {
        String src = read("race_solution.pep");
        for (int q : QUANTA) {
            assertEquals(REPORT_A + REPORT_B, run(src, q), "report, quantum " + q);
        }
    }

    @Test
    @DisplayName("Starter with no timer: works, because the threads never interrupt each other")
    void starterWorksWithoutTimer() throws IOException {
        assertEquals(REPORT_A + REPORT_B, run(read("race_starter.pep"), 0));
    }

    @Test
    @DisplayName("Starter: some quantum garbles a number (the shared static variables)")
    void starterIsBroken() throws IOException {
        String src = read("race_starter.pep");
        int broken = 0;
        for (int q : QUANTA) {
            if (!correct(run(src, q))) {
                broken++;
            }
        }
        assertTrue(broken >= 5, "only " + broken + " quanta exposed the bug");
    }

    @Test
    @DisplayName("Starter and solution differ only inside the student block")
    void filesDifferOnlyInStudentBlock() throws IOException {
        String starter = read("race_starter.pep");
        String solution = read("race_solution.pep");
        String begin = "; ---- BEGIN STUDENT CODE";
        String end = "; ---- END STUDENT CODE";
        assertEquals(starter.substring(0, starter.indexOf(begin)), solution.substring(0, solution.indexOf(begin)),
                "code before the student block");
        assertEquals(starter.substring(starter.indexOf(end)), solution.substring(solution.indexOf(end)),
                "code after the student block");
    }
}
