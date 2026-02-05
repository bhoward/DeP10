package edu.depauw.dep10.preprocess;

import java.util.List;

import edu.depauw.dep10.Value;

public record Line(String label, String command, List<Value> args, String comment, Log log) {
	public static Line of(String label, String command, List<Value> args, String comment, boolean visible) {
		return new Line(label, command, args, comment, new Log(visible));
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

	public void setVisible(boolean visible) {
	    log.setVisible(visible);
	}
	
    public boolean isVisible() {
        return log.isVisible();
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