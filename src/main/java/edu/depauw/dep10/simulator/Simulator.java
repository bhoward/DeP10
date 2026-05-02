package edu.depauw.dep10.simulator;

import edu.depauw.dep10.ModeOperation;
import edu.depauw.dep10.OpTable;
import edu.depauw.dep10.Operation;
import edu.depauw.dep10.PrefixEntry;
import edu.depauw.dep10.util.UByte;

public class Simulator {
	private State s;
	
	private OpTable table;
	
	public Simulator(State s) {
	    this.s = s;
	    this.table = new OpTable();
	    
	    Operation.installAll(table);
	    
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
			
			var entry = table.get(opcode);
			if (entry instanceof PrefixEntry prefix) {
			    s.setPrefix(opcode);
			    opcode = s.mem1(nextpc);
			    nextpc = nextpc.plus(1);
			    entry = prefix.get(opcode);
			} else {
			    s.setPrefix(UByte.of(0));
			}
			
			s.setOpCode(opcode);
			
			var op = (ModeOperation) entry;
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
