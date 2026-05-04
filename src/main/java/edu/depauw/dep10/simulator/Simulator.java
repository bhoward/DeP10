package edu.depauw.dep10.simulator;

import edu.depauw.dep10.op.Pep10;

public class Simulator {
	private State s;
	
	public Simulator(State s) {
	    this.s = s;
	    
        initialize();
	}

    private void initialize() {
        s.setPC(s.mem2(Pep10.DISPATCHER_POINTER));
        s.setSP(s.mem2(Pep10.SYSTEM_STACK_POINTER));
    }
    
    public void run(Controller control) {
        while (s.isRunning()) {
            var pc = s.getPC();
            Pep10.table.perform(s, pc, control);
        }
    }
}
