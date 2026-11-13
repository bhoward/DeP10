package edu.depauw.dep10.simulator2;

public interface DebugState extends State {
    Step getStep();

    void record(Action action);
}
