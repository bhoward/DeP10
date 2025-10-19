package edu.depauw.dep10;

import java.util.List;

public record Macro(String name, int numArgs, List<Line> body) {
	public List<Line> instantiate(List<Arg> args) {
		// TODO return a copy of body with substituted args for $1, $2, etc.
		return null;
	}
}
