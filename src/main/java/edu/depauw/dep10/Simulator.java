package edu.depauw.dep10;

public class Simulator {
	// TODO loading and initialization
	
	private State s;
	
	private OpTable table;
	
	public void run() {
		s.setPC(Operation.DISPATCHER_POINTER);
		s.setSP(Operation.SYSTEM_STACK_POINTER);
		
		while (s.isRunning()) {
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
