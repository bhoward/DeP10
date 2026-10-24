package edu.depauw.dep10.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static edu.depauw.dep10.resources.PepHarness.mem;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks the files for the heap allocator exercise in demo/heap: the starter (HFree does nothing)
 * and the instructor solution. The library part of the solution (everything before the DRIVER
 * marker) is also run with drivers written here, to check splitting, first fit, rounding,
 * exact fits and running out of memory. Results are read from scratch words at 0xF000 and up;
 * a payload address is stored as its offset from the start of the heap (0 for a failure).
 */
class HeapAllocatorTest {
    private static final int LIMIT = 100_000;
    private static final String MARKER = "; ---- DRIVER BELOW";

    private static String read(String name) throws IOException {
        return Files.readString(Path.of("demo", "heap", name));
    }

    private static String library(String file) throws IOException {
        String src = read(file);
        return src.substring(0, src.indexOf(MARKER));
    }

    /**
     * A driver made of steps: "a N" allocates N bytes and records the offset, "f K" frees the
     * K-th recorded allocation (counting from 1). Offsets go to 0xF000, 0xF002, ... in order.
     */
    private static int[] run(String lib, String... steps) {
        StringBuilder d = new StringBuilder("main:     CALL    HInit,i\n");
        int slot = 0;
        int[] recorded = new int[steps.length];
        int n = 0;
        for (String step : steps) {
            String[] w = step.split(" ");
            if (w[0].equals("a")) {
                d.append("          LDWA    ").append(w[1]).append(",i\n          CALL    HAlloc,i\n");
                d.append("          STWX    ").append(0xF100 + 2 * n).append(",d\n"); // raw address
                d.append("          CPWX    0,i\n          BREQ    z").append(n).append("\n");
                d.append("          SUBX    heap,i\n");
                d.append("z").append(n).append(":       STWX    ").append(0xF000 + 2 * n).append(",d\n");
                recorded[slot++] = n;
                n++;
            } else {
                int k = Integer.parseInt(w[1]) - 1;
                d.append("          LDWX    ").append(0xF100 + 2 * recorded[k]).append(",d\n          CALL    HFree,i\n");
            }
        }
        d.append("          LDBA    0,i\n          STBA    pwrOff,d\n          .END\n");
        var s = PepHarness.run(lib + MARKER + "\n" + d, LIMIT);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = mem(s, 0xF000 + 2 * i);
        }
        return out;
    }

    @Test
    @DisplayName("Starter as given: freeing does nothing, so the fourth request fails")
    void starterLeaks() throws IOException {
        assertEquals("p1=2\np2=14\np3=36\np4=0\n", PepHarness.runForOutput(read("heap_starter.pep"), LIMIT));
    }

    @Test
    @DisplayName("Solution: the freed 20 bytes are reused by the fourth request")
    void solutionReuses() throws IOException {
        assertEquals("p1=2\np2=14\np3=36\np4=14\n", PepHarness.runForOutput(read("heap_solution.pep"), LIMIT));
    }

    @Test
    @DisplayName("Blocks are laid out one after another, each preceded by a 2-byte header")
    void layout() throws IOException {
        assertEquals("[2, 8, 14]", java.util.Arrays.toString(run(library("heap_solution.pep"), "a 4", "a 4", "a 8")));
    }

    @Test
    @DisplayName("Requests are rounded up to an even size, with a minimum of 2")
    void rounding() throws IOException {
        assertEquals("[2, 8, 12]", java.util.Arrays.toString(run(library("heap_solution.pep"), "a 3", "a 0", "a 2")));
    }

    @Test
    @DisplayName("Out of memory returns 0: the whole heap is one 62-byte block")
    void exhaustion() throws IOException {
        assertEquals("[2, 0]", java.util.Arrays.toString(run(library("heap_solution.pep"), "a 62", "a 2")));
        assertEquals("[2, 0]", java.util.Arrays.toString(run(library("heap_solution.pep"), "a 60", "a 4")));
        assertEquals("[0]", java.util.Arrays.toString(run(library("heap_solution.pep"), "a 63")));
    }

    @Test
    @DisplayName("First fit: the first free block that is big enough is used, even if a later one fits better")
    void firstFit() throws IOException {
        // blocks: 20 at 2, 4 at 24, 10 at 30, 4 at 42; free the 20 and the 10, then ask for 6
        int[] r = run(library("heap_solution.pep"), "a 20", "a 4", "a 10", "a 4", "f 1", "f 3", "a 6");
        assertEquals("[2, 24, 30, 42, 2]", java.util.Arrays.toString(r));
    }

    @Test
    @DisplayName("A freed block is split when a smaller request reuses it, and the rest stays usable")
    void splitFreedBlock() throws IOException {
        int[] r = run(library("heap_solution.pep"), "a 20", "a 4", "f 1", "a 6", "a 8", "a 2");
        // the 20-byte block at 2 is split into 6 bytes at 2 and a free 12-byte block at 10;
        // 8 bytes of that go at 10, leaving a free 2-byte block at 20, which the last request takes
        assertEquals("[2, 24, 2, 10, 20]", java.util.Arrays.toString(r));
    }

    @Test
    @DisplayName("When a leftover is too small for a block of its own, the whole block is used and later blocks stay intact")
    void exactFitNoSplit() throws IOException {
        // free 12, ask for 10: leftover 2 bytes cannot hold a header plus payload
        int[] r = run(library("heap_solution.pep"), "a 12", "a 6", "f 1", "a 10", "a 6", "a 2");
        assertEquals(2, r[0]);
        assertEquals(16, r[1]);
        assertEquals(2, r[2], "the 12-byte block is reused whole");
        assertEquals(24, r[3], "the next request goes after the second block, so the walk steps over the 12-byte block");
    }

    @Test
    @DisplayName("Starter and solution differ only inside the student block")
    void filesDifferOnlyInStudentBlock() throws IOException {
        String starter = read("heap_starter.pep");
        String solution = read("heap_solution.pep");
        String begin = "; ---- BEGIN STUDENT CODE";
        String end = "; ---- END STUDENT CODE";
        assertEquals(starter.substring(0, starter.indexOf(begin)), solution.substring(0, solution.indexOf(begin)));
        assertEquals(starter.substring(starter.indexOf(end)), solution.substring(solution.indexOf(end)));
    }
}
