package edu.depauw.dep10.simulator2;

public class PlainController extends AbstractController {
	public PlainController(Machine machine) {
		super(machine);
	}

	@Override
	protected void doCycle() {
		while (state.isRunning()) {
			var op = machine.fetch(state);
			op.execute(state);
		}
	}
}
