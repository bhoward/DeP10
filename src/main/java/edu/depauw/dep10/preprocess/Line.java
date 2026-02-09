package edu.depauw.dep10.preprocess;

import java.util.List;

import edu.depauw.dep10.UByte;
import edu.depauw.dep10.assemble.Value;

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
		
    public void add(UByte uByte) {
        log.addByte(uByte);
    }

    public List<UByte> getBytes() {
        return log.getBytes();
    }

    public String toListing(int address) {
        return log.produceListing(this, address);
    }

    public boolean hasErrors() {
        return log.hasErrors();
    }
    
    public String getErrors() {
        return log.getErrors();
    }
}