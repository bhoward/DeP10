package edu.depauw.dep10.preprocess;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import edu.depauw.dep10.UByte;

public class Log {
    private static final int INITIAL_BYTES = 3;
    private static final int LABEL_START = 15;
	private static final int COMMAND_START = 27;
	private static final int ARG_START = 36;
	private static final int COMMENT_START = 48;
	
	private List<String> messages;
	private boolean makeComment;
	private boolean visible;
	private String source;
	private int lineNumber;
	private List<UByte> bytes;
	
	public Log(boolean visible) {
		this.messages = new ArrayList<>();
		this.makeComment = false;
		this.visible = visible;
		this.bytes = new ArrayList<>();
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

	public String produceListing(Line line, int address) {
		var builder = new StringBuilder();
        var format = HexFormat.of().withUpperCase();
		
        builder.append(format.toHexDigits((short) address));
        builder.append(' ');
        
		for (int i = 0; i < INITIAL_BYTES; i++) {
		    if (i < bytes.size()) {
		        format.toHexDigits(builder, (byte) bytes.get(i).value());
		        builder.append(' ');
		    }
		}
		alignToColumn(builder, LABEL_START);
		
		builder.append((line.label().isEmpty()) ? "" : line.label() + ":");
		alignToColumn(builder, COMMAND_START);
		
		if (makeComment) {
			builder.append(';');
		}
		
		builder.append(line.command().toUpperCase());
		alignToColumn(builder, ARG_START);
		
		if (line.args() != null && line.args().size() > 0) {
			var args = line.args();
			builder.append(args.get(0));
			for (int i = 1; i < args.size(); i++) {
				builder.append(',');
				builder.append(args.get(i));
			}
		}
		alignToColumn(builder, COMMENT_START);
		
		builder.append(line.comment());
		
		if (!messages.isEmpty()) {
		    builder.append("; " + source + "[" + lineNumber + "]");
		    for (var message : messages) {
		        builder.append(" ");
		        builder.append(message);
		    }
		}
		
		builder.append('\n');
		
	    for (int i = INITIAL_BYTES; i < bytes.size(); i += INITIAL_BYTES) {
	        builder.append(format.toHexDigits((short) (address + i)));
	        builder.append(' ');

	        for (int j = i; j < i + INITIAL_BYTES; j++) {
	            if (j < bytes.size()) {
	                format.toHexDigits(builder, (byte) bytes.get(j).value());
	                builder.append(' ');
	            }
	        }
	        builder.append('\n');
	    }
		
		return builder.toString();
	}

    private void alignToColumn(StringBuilder builder, int column) {
        builder.repeat(' ', Math.max(column - builder.length(), 0));
    }

    public void addByte(UByte uByte) {
        bytes.add(uByte);
    }

    public List<UByte> getBytes() {
        return bytes;
    }

    public boolean hasErrors() {
        return !messages.isEmpty();
    }
    
    public String getErrors() {
        StringBuilder builder = new StringBuilder();
        
        for (var message : messages) {
            builder.append(source + "[" + lineNumber + "] " + message + "\n");
        }
        
        return builder.toString();
    }
}

