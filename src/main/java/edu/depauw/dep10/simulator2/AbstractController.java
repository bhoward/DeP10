package edu.depauw.dep10.simulator2;

import java.util.function.Consumer;

/**
 * Implements the basic lifecycle activities for a simulation: running, pausing,
 * and resuming.
 */
public abstract class AbstractController implements Controller {
    protected Machine machine;
    protected Image image;
    protected State state;

    private Thread thread;

    public AbstractController(Machine machine, Image image) {
        this.machine = machine;
        this.image = image;
        this.state = null;
        this.thread = null;
    }

    @Override
    public void run(Consumer<State> andThen) {
        pause(); // stop any previous run

        state = machine.init(image);
        state.run();
        startCycle(andThen);
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
    public void resume(Consumer<State> andThen) {
        if (isPaused()) {
            state.resume();
            startCycle(andThen);
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

    protected void startCycle(Consumer<State> andThen) {
        thread = new Thread(() -> {
            doCycle();

            if (andThen != null) {
                andThen.accept(state);
            }
        });
        thread.start();
    }

    abstract protected void doCycle();

    protected void doStep() {
        try {
            var op = machine.fetch(state);
            state.setOperation(op);
            op.execute(state);
            state.tick();
        } catch (Fault fault) {
            machine.handle(fault, state);
        }
    }
}
