package edu.depauw.dep10.simulator2;

public interface Machine {

	State init();

	Operation fetch(State state);

}
