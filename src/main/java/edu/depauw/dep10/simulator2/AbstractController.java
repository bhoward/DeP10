package edu.depauw.dep10.simulator2;

public abstract class AbstractController implements Controller {
	protected Machine machine;
	protected State state;

	private Thread thread;

	public AbstractController(Machine machine) {
		this.machine = machine;
		this.state = null;
		this.thread = null;
	}

	@Override
	public void run() {
		pause(); // stop any previous run

		state = machine.init();
		startCycle();
	}

	@Override
	public void pause() {
		if (state != null) {
			state.pause();
		}

		if (thread != null) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				// Ignore the interruption
			}
			
			thread = null;
		}
	}

	@Override
	public void resume() {
		if (state != null && thread == null) {
			state.resume();
			startCycle();
		}
	}

	private void startCycle() {
		thread = new Thread(() -> doCycle());
		thread.start();
	}

	abstract protected void doCycle();
}
