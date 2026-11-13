package edu.depauw.dep10.simulator2;

public abstract class AbstractState implements State {
    private boolean running;
    
    public AbstractState() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        running = true;
    }

    @Override
    public void pause() {
        running = false;
    }

    @Override
    public void resume() {
        running = true;
    }
}
