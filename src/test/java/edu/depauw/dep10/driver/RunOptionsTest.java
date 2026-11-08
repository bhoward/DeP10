package edu.depauw.dep10.driver;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.depauw.dep10.resources.PepHarness;
import edu.depauw.dep10.simulator.State;

/** Which OS the run command loads: nothing for --sys, the bare metal image for --bm, the full OS by default. */
class RunOptionsTest {
    /** The words of the top page of memory, where the OS images put their vectors and code. */
    private static int[] topPage(State state) {
        // Reading 0xFFFD is a read of the console input port, which would block on System.in.
        state.setInput(new java.io.ByteArrayInputStream(new byte[0]));
        int[] words = new int[128];
        for (int i = 0; i < words.length; i++) {
            words[i] = PepHarness.mem(state, 0xFF00 + 2 * i);
        }
        return words;
    }

    private static final int[] FRESH = topPage(new State());

    private static State load(CommandRun run, ErrorLog log) {
        var state = new State();
        OsLoader.loadOS(run, state, log);
        return state;
    }

    @Test
    @DisplayName("--sys loads no OS at all")
    void sysLoadsNothing() {
        var run = new CommandRun();
        run.noOS = true;
        var log = new ErrorLog();
        assertArrayEquals(FRESH, topPage(load(run, log)));
        assertTrue(log.noErrors());
    }

    @Test
    @DisplayName("--bm loads the bare metal image and the default loads the full OS")
    void otherOptionsLoadAnOS() {
        var bm = new CommandRun();
        bm.bareMetal = true;
        assertFalse(Arrays.equals(FRESH, topPage(load(bm, new ErrorLog()))));

        var full = new State();
        OsLoader.loadOS(new CommandRun(), full, new ErrorLog());
        assertFalse(Arrays.equals(FRESH, topPage(full)));
    }

    @Test
    @DisplayName("--sys excludes --bm and --os")
    void sysExcludesTheOthers() {
        var both = new CommandRun();
        both.noOS = true;
        both.bareMetal = true;
        var log = new ErrorLog();
        load(both, log);
        assertFalse(log.noErrors());

        var named = new CommandRun();
        named.noOS = true;
        named.osName = "FULL";
        log = new ErrorLog();
        load(named, log);
        assertFalse(log.noErrors());
    }
}
