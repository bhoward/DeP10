package edu.depauw.dep10.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks demo/prodcons/prodcons.pep: a producer and a consumer sharing a two-slot buffer through two semaphores. */
class ProducerConsumerTest {
    private static final int LIMIT = 100_000;
    private static final String EMPTY_P = "LDWA    empty,i\n         SCALL   2,i";

    private static String demo() throws IOException {
        return Files.readString(Path.of("demo", "prodcons", "prodcons.pep"));
    }

    @Test
    @DisplayName("The producer never gets more than two items ahead, and the consumer sees 1 to 6 in order")
    void boundedBufferInOrder() throws IOException {
        String out = PepHarness.runForOutput(demo(), LIMIT);
        assertEquals("p1\np2\nc1\nc2\np3\np4\nc3\nc4\np5\np6\nc5\nc6\n", out);
        int made = 0, taken = 0;
        for (String line : out.split("\n")) {
            if (line.startsWith("p")) {
                made++;
            } else {
                taken++;
                assertEquals(taken, line.charAt(1) - '0', "items arrive in order");
            }
            assertTrue(made - taken >= 0 && made - taken <= 2, "buffer holds 0 to 2 items after " + line);
        }
        assertEquals(6, taken);
    }

    @Test
    @DisplayName("With no free slot to start with, both sides block and the machine stops")
    void noSlotsDeadlocks() throws IOException {
        String stuck = demo().replace("empty:   .WORD   2", "empty:   .WORD   0");
        assertFalse(stuck.equals(demo()));
        assertEquals("", PepHarness.runForOutput(stuck, LIMIT));
    }

    @Test
    @DisplayName("Without P(empty) the producer runs far ahead of the consumer")
    void withoutEmptySemaphoreProducerRunsAhead() throws IOException {
        String racy = demo().replace(EMPTY_P, "LDWA    empty,i\n         NOP\n         NOP");
        assertFalse(racy.equals(demo()), "the edit must change the program");
        String out = PepHarness.runForOutput(racy, LIMIT);
        assertTrue(out.startsWith("p1\np2\np3\np4\np5\np6\n"),
                "all six items were made before the consumer ran: " + out.replace('\n', ' '));
        assertFalse(out.contains("c1"), "item 1 was overwritten before it could be taken");
    }
}
