package edu.depauw.dep10;

public record ModeOperation(AddrMode mode, Operation op) {
	public boolean hasOperand() {
		return op.hasOperand();
	}
	
	public void apply(State s) {
		op.apply(s, mode);
	}
}
