package edu.depauw.dep10.preprocess;

import java.util.ArrayList;
import java.util.List;

public class Log {
	private static final int COMMAND_START = 12;
	private static final int ARG_START = 20;
	private static final int COMMENT_START = 32;
	
	private List<String> messages;
	private boolean makeComment;
	private boolean visible;
	private String source;
	private int lineNumber;
	
	public Log(boolean visible) {
		this.messages = new ArrayList<>();
		this.makeComment = false;
		this.visible = visible;
	}

	public void add(String message) {
		messages.add(message);
	}

	public void setComment() {
		makeComment = true;
	}
	
	public boolean isCommented() {
	    return makeComment;
	}

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isVisible() {
        return visible;
    }
	
	public void setSource(String source) {
	    this.source = source;
	}
	
	public void setLineNumber(int lineNumber) {
	    this.lineNumber = lineNumber;
	}

	public String produceListing(Line line) {
		var builder = new StringBuilder();
		
		builder.append((line.label().isEmpty()) ? "" : line.label() + ":");
		builder.repeat(' ', Math.max(COMMAND_START - builder.length(), 0));
		
		if (makeComment) {
			builder.append(';');
		}
		
		builder.append(line.command().toUpperCase());
		builder.repeat(' ', Math.max(ARG_START - builder.length(), 0));
		
		if (line.args() != null && line.args().size() > 0) {
			var args = line.args();
			builder.append(args.get(0));
			for (int i = 1; i < args.size(); i++) {
				builder.append(',');
				builder.append(args.get(i));
			}
		}
		builder.repeat(' ', Math.max(COMMENT_START - builder.length(), 0));
		
		builder.append(line.comment());
		
		if (!messages.isEmpty()) {
		    builder.append("; " + source + "[" + lineNumber + "]");
		    for (var message : messages) {
		        builder.append(" ");
		        builder.append(message);
		    }
		}
		
		return builder.toString();
	}
}
