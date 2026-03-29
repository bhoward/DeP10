package edu.depauw.dep10.simulator;

import edu.depauw.dep10.OpTable;
import edu.depauw.dep10.Operation;

public class Simulator {
	private State s;
	
	private OpTable table;
	
	public Simulator(State s) {
	    this.s = s;
	    this.table = new OpTable();
	    
        initialize();
	}

    private void initialize() {
        s.setPC(s.mem2(Operation.DISPATCHER_POINTER));
        s.setSP(s.mem2(Operation.SYSTEM_STACK_POINTER));
    }
	
	public void run(Controller control) {
		while (s.isRunning()) {
			var pc = s.getPC();
			var opcode = s.mem1(pc);
			var nextpc = pc.plus(1);
			s.setOpCode(opcode);
			
			var op = table.get(opcode);
			if (op.hasOperand()) {
				var operand = s.mem2(nextpc);
				nextpc = nextpc.plus(2);
				s.setOperand(operand);
			}
			
			s.setPC(nextpc);
			control.perform(op, s, pc);
		}
	}
}
