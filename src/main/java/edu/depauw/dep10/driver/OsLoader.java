package edu.depauw.dep10.driver;

import static edu.depauw.dep10.driver.Driver.BARE_METAL_OS_OBJECT;
import static edu.depauw.dep10.driver.Driver.FORTH_OS_OBJECT;
import static edu.depauw.dep10.driver.Driver.FULL_OS_OBJECT;

import edu.depauw.dep10.simulator.State;

/** Chooses the OS that the run command loads. Kept apart from Driver so that tests do not load the GUI classes. */
final class OsLoader {
    private OsLoader() {
    }

    /**
     * Loads the object code of the OS chosen by the options: nothing for --sys, the bare metal OS for --bm,
     * the named OS for --os, and the full OS if none of them is given.
     */
    static void loadOS(CommandRun run, State state, ErrorLog log) {
        if (run.noOS) {
            if (run.bareMetal || run.osName != null) {
                log.error("--sys (no OS) excludes --bm and --os.");
            }
            // load nothing: the program brings its own kernel
        } else if (run.bareMetal) {
            if (run.osName != null) {
                log.error("Bare metal excludes specifying OS.");
            }
            state.loadResource(BARE_METAL_OS_OBJECT);
        } else if (run.osName != null) {
            if (run.osName.equalsIgnoreCase("FULL")) {
                state.loadResource(FULL_OS_OBJECT);
            } else if (run.osName.equalsIgnoreCase("FORTH")) {
                state.loadResource(FORTH_OS_OBJECT);
            } else {
                state.loadFile(run.osName + ".pepo");
            }
        } else {
            state.loadResource(FULL_OS_OBJECT);
        }
    }
}
