package edu.depauw.dep10;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import edu.depauw.dep10.preprocess.Line;

public class Section {
	private static final int BYTES_PER_OBJECT_FILE_LINE = 8;
	
	private Result parent;
    private int origin;
	private int nextAddress;
	private List<Line> lines;
	private Line current;
	private Map<String, Value> locals;
	private List<ObjectEntry> objects;
	
	private record ObjectEntry(Value value, Line line) {}

	public Section(Result parent) {
	    this.parent = parent;
		this.origin = 0;
		this.nextAddress = 0;
		this.lines = new ArrayList<>();
		this.locals = new HashMap<>();
		this.objects = new ArrayList<>();
	}

    public void addLine(Line line) {
        lines.add(line);
        current = line;
    }
    
    public void addLabel(String label) {
        locals.put(label, new Value.RelativeNumber(this, nextAddress));
    }

    public void equate(String label, Value value) {
        locals.put(label, value);
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

    public Value lookup(String sym) {
        if (locals.containsKey(sym)) {
            return locals.get(sym);
        } else {
            return parent.lookup(sym);
        }
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
                        line.add(new UByte(0));
                    }
                    break;
                }

                case Value.CharLit(var c): {
                    line.add(new UByte(c));
                    break;
                }

                case Value.LowByte(var v): {
                    var w = v.evaluate(this);
                    line.add(w.lo());
                    break;
                }

                case Value.StrLit(var s): {
                    for (int i = 0; i < s.length(); i++) {
                        line.add(new UByte(s.charAt(i)));
                    }
                    break;
                }

                default: {
                    var w = entry.value.evaluate(this);
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
}
