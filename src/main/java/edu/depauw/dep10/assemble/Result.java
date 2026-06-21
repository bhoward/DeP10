package edu.depauw.dep10.assemble;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;

import edu.depauw.dep10.driver.ErrorLog;
import edu.depauw.dep10.preprocess.Line;
import edu.depauw.dep10.preprocess.Log;
import edu.depauw.dep10.util.Util;

public class Result {
    private List<Section> sections;
    private Section current;
    private Map<String, Value> symbols;
    private List<String> exports;
    
    public Result() {
        this.sections = new ArrayList<Section>();
        this.current = new Section(this);
        this.symbols = new HashMap<>();
        this.exports = new ArrayList<>();
        
        sections.add(current);
    }
    
    public void addLine(Line line) {
        current.addLine(line);
    }
    
    public void addLabel(String label) {
    	symbols.put(label, new Value.RelativeNumber(current, current.size()));
    }

    public String toObjectFile() {
        StringBuilder builder = new StringBuilder();
        
        for (var section : sections) {
            builder.append(section.toObjectFile(this));
            builder.append('\n');
        }
        
        return builder.toString();
    }

    public void equate(String label, Value value) {
    	symbols.put(label, value);
    }

    public void align(int n) {
        current.align(n);
    }
    
    public void export(String sym) {
        exports.add(sym);
    }

    public void addObject(Value value) {
        current.addObject(value);
    }
    
    public void org(int n) {
        current.org(n);
    }

    public void section(String name, String flags, ErrorLog log) {
        Line line = current.removeLastLine();
        
        current = new Section(this); // TODO use the name and flags...
        sections.add(current);
        current.addLine(line);
    }

    public Value lookup(String sym) {
        return symbols.get(sym);
    }

    public void resolveObjects(ErrorLog log) {
        // Pack the sections:
        // * First section (user) goes at 0, unless explicitly relocated
        // * Remaining sections pack to top of memory, unless explicitly relocated
        // * Bad luck if there is overlap...
        int top = 65536;
        for (int i = sections.size() - 1; i > 0; i--) {
            var section = sections.get(i);
            
            if (section.getOrigin() == 0) {
                top = top - section.size();
                section.setOrigin(top);
            } else {
                // Respect explicit origin
                top = section.getOrigin();
            }
        }
        
        for (var section : sections) {
            section.resolveObjects();
        }
    }

    public void printListing(PrintWriter out) {
        for (var section : sections) {
            section.printListing(out);
        }
        
        printSymbols(out);
    }
    
    public void printSymbols(PrintWriter out) {
        var syms = new ArrayList<>(symbols.keySet());
        Collections.sort(syms);
        int width = syms.stream().map(s -> s.length()).max(Integer::compare).get();
        
        out.println();
        out.println("Symbol Table");
        out.println("------------");
        for (var sym : syms) {
            StringBuilder builder = new StringBuilder();
            builder.append(sym);
            builder.repeat(' ', Math.max(width - sym.length(), 0));
            builder.append(" = ");
            builder.append(Util.HEX_FORMAT.toHexDigits(Integer.valueOf(lookup(sym).toString()).shortValue()));
            out.println(builder.toString());
        }
    }

    public void printErrors(PrintWriter out) {
        for (var section : sections) {
            section.printErrors(out);
        }
    }

    public void printHeader(PrintWriter out) {
        for (var symbol : exports) {
            out.println(symbol + ": .EQUATE " + lookup(symbol));
        }
    }

    public Document getListingDocument() {
        StyledDocument document = new DefaultStyledDocument();
        for (var section : sections) {
            section.appendListing(document);
        }
        
        var writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        printSymbols(out);
        out.close();
        try {
            document.insertString(document.getLength(), writer.toString(), Log.DEFAULT_ATTRS);
        } catch (BadLocationException e) {
            // Should not happen
        }
        
        return document;
    }

    public boolean hasErrors() {
        for (var section : sections) {
            if (section.hasErrors()) {
                return true;
            }
        }
        
        return false;
    }
}
