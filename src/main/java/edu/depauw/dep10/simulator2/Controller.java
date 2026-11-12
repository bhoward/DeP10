package edu.depauw.dep10.simulator2;

public interface Controller {
	/**
	 * Start program execution from an initial machine state. Runs asynchronously.
	 * This will attempt to stop any previously running execution.
	 */
	void run();

	/**
	 * Pauses the currently running execution, if any.
	 */
	void pause();

	/**
	 * Resumes a previously paused execution, if any. 
	 */
	void resume();
}
