package edu.depauw.dep10.simulator2;

import java.util.ArrayList;
import java.util.List;

public class DebugController extends AbstractController {
	private List<Breakpoint> breakpoints;
	
	public DebugController(Machine machine) {
		super(machine);
		this.breakpoints = new ArrayList<>();
	}
	
	public void clearBreakpoints() {
		breakpoints.clear();
	}
	
	public void addBreakPoint(Breakpoint breakpoint) {
		breakpoints.add(breakpoint);
	}

	@Override
	protected void doCycle() {
		// TODO Auto-generated method stub
		
	}
}
