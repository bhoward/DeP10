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
		state.run();
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
		if (isPaused()) {
			state.resume();
			startCycle();
		}
	}

	@Override
    public boolean isRunning() {
        return state != null && thread != null;
    }

    @Override
    public boolean isPaused() {
        return state != null && thread == null;
    }

    protected void startCycle() {
		thread = new Thread(() -> doCycle());
		thread.start();
	}

	abstract protected void doCycle();

	protected void doStep() {
		try {
			var op = machine.fetch(state);
			op.execute(state);
		} catch (Fault fault) {
			machine.handle(fault, state);
		}
	}
}
