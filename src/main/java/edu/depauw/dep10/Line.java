package edu.depauw.dep10;

import java.util.List;

public record Line(String label, String command, List<Arg> args, String comment) {
	public boolean isEmpty() {
		return label.isEmpty() && command.isEmpty();
	}
}