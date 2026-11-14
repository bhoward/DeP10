package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.simulator2.common.RegisterFile;

public class DebugBaseState extends BaseState implements DebugState {

	public DebugBaseState(Device memory, RegisterFile registerFile) {
		super(memory, registerFile);
	}

	@Override
	public Step getStep() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void record(Action action) {
		// TODO Auto-generated method stub

	}

}
