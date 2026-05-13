package edu.depauw.dep10.preprocess;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import edu.depauw.dep10.util.UByte;

public class Log {
    private static final int INITIAL_BYTES = 4;
    private static final int LABEL_START = 18;
    private static final int COMMAND_START = 30;
    private static final int ARG_START = 40;
    private static final int COMMENT_START = 52;

    public static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
    public static final MutableAttributeSet DEFAULT_ATTRS = new SimpleAttributeSet();
    public static final MutableAttributeSet ERROR_ATTRS = new SimpleAttributeSet();

    static {
        StyleConstants.setForeground(DEFAULT_ATTRS, Color.BLACK);
        StyleConstants.setForeground(ERROR_ATTRS, Color.RED);
    }

    private List<String> messages;
    private boolean makeComment;
    private boolean visible;
    private String source;
    private int lineNumber;
    private List<UByte> bytes;

    public Log() {
        this.messages = new ArrayList<>();
        this.makeComment = false;
        this.visible = true;
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

        formatFirstLine(line, address, builder);

        if (!messages.isEmpty()) {
            formatMessages(builder);
        }
        builder.append('\n');

        if (bytes.size() > INITIAL_BYTES) {
            formatRemainingLines(address, builder);
        }

        return builder.toString();
    }

    public void appendListing(StyledDocument document, Line line, int address) {
        var builder = new StringBuilder();
        formatFirstLine(line, address, builder);
        appendString(document, builder.toString(), DEFAULT_ATTRS);

        if (!messages.isEmpty()) {
            builder = new StringBuilder();
            formatMessages(builder);
            appendString(document, builder.toString(), ERROR_ATTRS);
        }
        appendString(document, "\n", DEFAULT_ATTRS);

        if (bytes.size() > INITIAL_BYTES) {
            builder = new StringBuilder();
            formatRemainingLines(address, builder);
            appendString(document, builder.toString(), DEFAULT_ATTRS);
        }
    }

    private void appendString(StyledDocument document, String string, AttributeSet attributes) {
        try {
            document.insertString(document.getLength(), string, attributes);
        } catch (BadLocationException e) {
            // should not happen
        }
    }

    public void formatFirstLine(Line line, int address, StringBuilder builder) {
        builder.append(HEX_FORMAT.toHexDigits((short) address));
        builder.append(' ');

        for (int i = 0; i < INITIAL_BYTES; i++) {
            if (i < bytes.size()) {
                HEX_FORMAT.toHexDigits(builder, (byte) bytes.get(i).value());
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
    }

    public void formatRemainingLines(int address, StringBuilder builder) {
        for (int i = INITIAL_BYTES; i < bytes.size(); i += INITIAL_BYTES) {
            builder.append(HEX_FORMAT.toHexDigits((short) (address + i)));
            builder.append(' ');

            for (int j = i; j < i + INITIAL_BYTES; j++) {
                if (j < bytes.size()) {
                    HEX_FORMAT.toHexDigits(builder, (byte) bytes.get(j).value());
                    builder.append(' ');
                }
            }
            builder.append('\n');
        }
    }

    public void formatMessages(StringBuilder builder) {
        builder.append("; " + source + "[" + lineNumber + "]");
        for (var message : messages) {
            builder.append(" ");
            builder.append(message);
        }
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
