package edu.depauw.dep10;

public class Simulator {
	private State s;
	
	private OpTable table;
	
	public Simulator(State s) {
	    this.s = s;
	    this.table = new OpTable();
	}
	
	public void run() {
		s.setPC(s.mem2(Operation.DISPATCHER_POINTER));
		s.setSP(s.mem2(Operation.SYSTEM_STACK_POINTER));
		
		while (s.isRunning()) {
		    // TODO count number of steps
			var pc = s.getPC();
			var opcode = s.mem1(pc);
			pc = pc.plus(1);
			s.setOpCode(opcode);
			
			var op = table.get(opcode);
			if (op.hasOperand()) {
				var operand = s.mem2(pc);
				pc = pc.plus(2);
				s.setOperand(operand);
			}
			
			s.setPC(pc);
			op.apply(s);
		}
	}
}
