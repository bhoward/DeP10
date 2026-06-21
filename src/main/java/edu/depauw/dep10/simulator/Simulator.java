package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Pep10;

public class Simulator {
	private State s;
	
	public Simulator(State s) {
	    this(s, true);
	}
	
	public Simulator(State s, boolean init) {
	    this.s = s;
	    if (init) {
	        s.initialize(Pep10.DISPATCHER_POINTER, Pep10.SYSTEM_STACK_POINTER);
	    }
	}
    
    public void run(Controller control) {
        while (s.isRunning()) {
            s.doStep(control, Pep10.table);
        }
    }
}
