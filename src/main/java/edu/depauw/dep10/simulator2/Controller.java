package edu.depauw.dep10.simulator2;

import java.util.function.Consumer;

/**
 * Represents the connection between the user and the simulation, handling
 * requests to run, pause, and resume.
 */
public interface Controller {
    /**
     * Start program execution from an initial machine state. Runs asynchronously.
     * This will attempt to stop any previously running execution. If the argument
     * is not null, it provides an action that will be run after execution halts.
     *
     * @param andThen the action to perform after the program halts, or null
     */
    void run(Consumer<State> andThen);

    /**
     * Pauses the currently running execution, if any.
     */
    void pause();

    /**
     * Resumes a previously paused execution, if any. If the argument
     * is not null, it provides an action that will be run after execution halts.
     *
     * @param andThen the action to perform after the program halts, or null
     */
    void resume(Consumer<State> andThen);

    /**
     * @return true if there is a currently running execution
     */
    boolean isRunning();

    /**
     * @return true if there is a current but paused execution
     */
    boolean isPaused();
}
