package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.simulator.StepCountController;
import edu.depauw.dep10.util.Word;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Checks the "Address Spaces" files in demo/vm2: two processes store different values at the same
 * virtual address and each must read back its own, which needs a different page table per process.
 */
class AddressSpacesTest {
    private static final int LIMIT = 200_000;

    private static String read(String name) throws IOException {
        return Files.readString(Path.of("demo", "vm2", name));
    }

    private static State load(String src) {
        State s = PepHarness.load(PepHarness.assemble(src), null);
        s.setFancy(true);
        s.setOutput(new ByteArrayOutputStream());
        s.setError(new ByteArrayOutputStream());
        return s;
    }

    private static String run(String src) {
        State s = load(src);
        var out = new ByteArrayOutputStream();
        s.setOutput(out);
        new Simulator(s).run(new StepCountController(new PlainController(), LIMIT));
        assertFalse(s.isRunning(), "machine should have powered off");
        return out.toString(StandardCharsets.US_ASCII);
    }

    @Test
    @DisplayName("Solution: each process reads back its own value from the same virtual address")
    void solutionKeepsSpacesApart() throws IOException {
        assertEquals("P1\nQ2\nAll processes finished\n", run(read("vm2_solution.pep")));
    }

    @Test
    @DisplayName("Solution: the two values live in different frames, and physical page 4 is untouched")
    void valuesLiveInDifferentFrames() throws IOException {
        State s = load(read("vm2_solution.pep"));
        new Simulator(s).run(new StepCountController(new PlainController(), LIMIT));
        assertEquals(1, s.mem2Safe(Word.of(0x2000)).value());
        assertEquals(2, s.mem2Safe(Word.of(0x2100)).value());
        assertEquals(0, s.mem2Safe(Word.of(0x0400)).value());
    }

    @Test
    @DisplayName("Starter: with no table installed the processes share memory and P reads Q's value")
    void starterSharesMemory() throws IOException {
        assertEquals("P2\nQ2\nAll processes finished\n", run(read("vm2_starter.pep")));
    }

    @Test
    @DisplayName("Starter and solution differ only inside the student block")
    void filesDifferOnlyInStudentBlock() throws IOException {
        String starter = read("vm2_starter.pep");
        String solution = read("vm2_solution.pep");
        String begin = "; ---- BEGIN STUDENT CODE";
        String end = "; ---- END STUDENT CODE";
        assertEquals(starter.substring(0, starter.indexOf(begin)), solution.substring(0, solution.indexOf(begin)));
        assertEquals(starter.substring(starter.indexOf(end)), solution.substring(solution.indexOf(end)));
    }
}
