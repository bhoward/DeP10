package edu.depauw.dep10.preprocess;

import java.util.List;

import edu.depauw.dep10.Value;

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
	
	public boolean isCommented() {
	    return log.isCommented();
	}
	
	public void setLocation(String source, int lineNumber) {
	    log.setSource(source);
	    log.setLineNumber(lineNumber);
	}
		
	@Override
	public String toString() {
	    return log.produceListing(this);
	}
}