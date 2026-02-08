package edu.depauw.dep10;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.depauw.dep10.preprocess.Line;

public class Result {
    private List<Section> sections;
    private Section current;
    private Map<String, Value> globals;
    
    public Result() {
        this.sections = new ArrayList<Section>();
        this.current = new Section(this);
        this.globals = new HashMap<>();
        
        sections.add(current);
    }
    
    public void addLine(Line line) {
        current.addLine(line);
    }
    
    public void addLabel(String label) {
        current.addLabel(label);
    }

    public String toObjectFile() {
        StringBuilder builder = new StringBuilder();
        
        for (var section : sections) {
            builder.append(section.toObjectFile(this));
        }
        
        return builder.toString();
    }

    public void equate(String label, Value value) {
        current.equate(label, value);
    }

    public void align(int n) {
        current.align(n);
    }

    public void addObject(Value value) {
        current.addObject(value);
    }

    public void addGlobal(String sym) throws LineError {
        var value = current.lookup(sym);
        if (value != null) {
            globals.put(sym, value);
        } else {
            throw new LineError("Unknown symbol " + sym);
        }
    }

    public void org(int n) {
        current.org(n);
    }

    public Value lookup(String sym) {
        return globals.get(sym);
    }

    public void resolveObjects() {
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

    public void printListing(List<Line> lines, PrintWriter out) {
        for (var section : sections) {
            section.printListing(out);
        }
    }
}
