package edu.depauw.dep10.simulator2;

public interface Machine {
	State init();

    DebugState initDebug();

    Operation fetch(State state) throws Fault;

	void handle(Fault fault, State state);
}
