package edu.depauw.dep10;

import java.util.List;

public record Line(String label, String command, List<Value> args, String comment, Log log) {
	public static Line of(String label, String command, List<Value> args, String comment) {
		return new Line(label, command, args, comment, new Log());
	}
	
	public void logError(String message) {
		log.add(message);
	}
	
	public void setComment() {
		log.setComment();
	}
}