package edu.depauw.dep10.assemble;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import javax.swing.text.StyledDocument;

import edu.depauw.dep10.preprocess.Line;
import edu.depauw.dep10.util.UByte;

public class Section {
	private static final int BYTES_PER_OBJECT_FILE_LINE = 8;
	
	private Result parent;
	private String name = null;
	private String flags = null;
    private int origin;
	private int nextAddress;
	private List<Line> lines;
	private Line current;
	private List<ObjectEntry> objects;
	
	private record ObjectEntry(Value value, Line line) {}

	public Section(Result parent) {
	    this.parent = parent;
		this.origin = 0;
		this.nextAddress = 0;
		this.lines = new ArrayList<>();
		this.objects = new ArrayList<>();
	}

    /** Name and flags given by a .SECTION directive; null for the first section, which has no directive. */
    public void setNameAndFlags(String name, String flags) {
        this.name = name;
        this.flags = flags;
    }

    public String getName() {
        return name;
    }

    public String getFlags() {
        return flags;
    }

    /**
     * The two protection bits (read, write) for the flags of a section, as the memory protection
     * table stores them: 0 (00) no user access, 2 (10) read only, 3 (11) read and write, and
     * 1 (01) read and execute, which is how a code page is marked. An x flag makes the section
     * code, even with w (a page cannot be both writable and executable); r or w alone means data;
     * no r, w or x means no user access. Other letters (such as z) do not affect protection.
     */
    public static int protectionBits(String flags) {
        String f = flags == null ? "" : flags.toLowerCase();
        if (f.indexOf('x') >= 0) {
            return 1;
        } else if (f.indexOf('w') >= 0) {
            return 3;
        } else if (f.indexOf('r') >= 0) {
            return 2;
        }
        return 0;
    }

    public void addLine(Line line) {
        lines.add(line);
        current = line;
    }
    
    public void align(int n) {
        if (nextAddress % n != 0) {
            int skip = n - (nextAddress % n);
            addObject(new Value.Block(skip));
        }
    }

    public void addObject(Value value) {
        objects.add(new ObjectEntry(value, current));
        nextAddress += value.size();
    }

    public void org(int n) {
        origin = n - nextAddress;
    }
    
    public int size() {
        return nextAddress;
    }

    public void resolveObjects() {
        for (var entry : objects) {
            var value = entry.value;
            var line = entry.line;
            
            try {
                switch (value) {
                case Value.Block(var size): {
                    for (int i = 0; i < size; i++) {
                        line.add(UByte.of(0));
                    }
                    break;
                }

                case Value.CharLit(var c): {
                    line.add(UByte.of(c));
                    break;
                }

                case Value.LowByte(var v): {
                    var w = v.evaluate(parent);
                    line.add(w.lo());
                    break;
                }

                case Value.StrLit(var s): {
                    for (int i = 0; i < s.length(); i++) {
                        line.add(UByte.of(s.charAt(i)));
                    }
                    break;
                }

                default: {
                    var w = entry.value.evaluate(parent);
                    line.add(w.hi());
                    line.add(w.lo());
                    break;
                }
                }
            } catch (ValueError e) {
                current.logError(e.getMessage());
            }
        }
    }
    
    public String toObjectFile(Result result) {
        var builder = new StringBuilder();
        var format = HexFormat.of().withUpperCase();
        
        if (origin != 0) {
            builder.append("[" + format.toHexDigits(origin, 4) + "]\n");
        }
        
        var col = 0;
        for (var line : lines) {
            for (var b : line.getBytes()) {
                format.toHexDigits(builder, (byte) b.value());
                col++;
                if (col == BYTES_PER_OBJECT_FILE_LINE) {
                    builder.append('\n');
                    col = 0;
                } else {
                    builder.append(' ');
                }                
            }
        }
        
        return builder.toString();
    }

    public Line removeLastLine() {
        return lines.removeLast();
    }

    public int getOrigin() {
        return origin;
    }

    public void setOrigin(int origin) {
        this.origin = origin;
    }

    public void printListing(PrintWriter out) {
        int address = origin;
        
        for (var line : lines) {
            if (line.isVisible()) {
                out.print(line.toListing(address));
            }
            address += line.getBytes().size();
        }
    }

    public void printErrors(PrintWriter out) {
        for (var line : lines) {
            if (line.hasErrors()) {
                out.print(line.getErrors());
            }
        }
    }

    public void appendListing(StyledDocument document) {
        int address = origin;
        
        for (var line : lines) {
            if (line.isVisible()) {
                line.appendListing(document, address);
            }
            address += line.getBytes().size();
        }
    }

    public boolean hasErrors() {
        for (var line : lines) {
            if (line.hasErrors()) {
                return true;
            }
        }
        
        return false;
    }

    public int fillAddressMap(Map<Integer, Integer> addressMap, Map<Integer, Integer> lineMap, int lineNumber) {
        int address = origin;
        
        for (var line : lines) {
            if (line.isVisible()) {
                addressMap.put(lineNumber, address);
                lineMap.put(address, lineNumber);
                lineNumber++;
            }
            address += line.getBytes().size();
        }
        
        return lineNumber;
    }
}
