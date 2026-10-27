package edu.depauw.dep10.assemble;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.depauw.dep10.driver.ErrorLog;
import edu.depauw.dep10.preprocess.Line;
import edu.depauw.dep10.util.Util;

public class Result {
    private List<Section> sections;
    private Section current;
    private Map<String, Value> symbols;
    private List<String> exports;
    private Map<String, Map<Integer, Integer>> sourceMap;
    private Map<Integer, Integer> addressMap;
    private Map<Integer, Integer> lineMap;
    private int listingLine;

    public Result() {
        this.sections = new ArrayList<Section>();
        this.current = new Section(this);
        this.symbols = new HashMap<>();
        this.exports = new ArrayList<>();
        this.sourceMap = new HashMap<>();
        this.addressMap = null;
        this.lineMap = null;
        this.listingLine = 0;

        sections.add(current);
    }

    public void addLine(Line line) {
        current.addLine(line);

        if (line.isVisible()) {
            var source = line.getSource();
            var lineNumber = line.getLineNumber();
            if (source != null) {
                sourceMap.putIfAbsent(source, new HashMap<>());
                sourceMap.get(source).putIfAbsent(lineNumber, listingLine);
            }
            listingLine++;
        }
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

        current = new Section(this);
        current.setNameAndFlags(name, flags);
        sections.add(current);
        current.addLine(line);
    }

    public List<Section> getSections() {
        return List.copyOf(sections);
    }

    /**
     * The memory protection table for the sections that have flags (a .SECTION directive), after
     * the sections have been placed: 64 bytes, two bits per 256-byte page, the first page in the
     * high bits of the first byte. A page that two sections touch keeps the more permissive bits.
     * Pages that no flagged section touches stay 00 (no user access).
     */
    public byte[] protectionTable() {
        byte[] table = new byte[64];
        for (var section : sections) {
            if (section.getFlags() == null || section.size() == 0) {
                continue;
            }
            int bits = Section.protectionBits(section.getFlags());
            int first = section.getOrigin() >> 8;
            int last = (section.getOrigin() + section.size() - 1) >> 8;
            for (int page = first; page <= last && page < 256; page++) {
                int shift = 6 - 2 * (page % 4);
                int old = (table[page / 4] >> shift) & 3;
                int merged = old == 0 ? bits : (old == 3 || bits == 3 ? 3 : (old == bits ? old : 3));
                table[page / 4] = (byte) ((table[page / 4] & ~(3 << shift)) | (merged << shift));
            }
        }
        return table;
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

//    public Document getListingDocument() {
//        StyledDocument document = new DefaultStyledDocument();
//        for (var section : sections) {
//            section.appendListing(document);
//        }
//        
//        var writer = new StringWriter();
//        PrintWriter out = new PrintWriter(writer);
//        printSymbols(out);
//        out.close();
//        try {
//            document.insertString(document.getLength(), writer.toString(), Log.DEFAULT_ATTRS);
//        } catch (BadLocationException e) {
//            // Should not happen
//        }
//        
//        return document;
//    }

    public String getSymbols() {
        var writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        printSymbols(out);
        out.close();
        return writer.toString();
    }

    public boolean hasErrors() {
        for (var section : sections) {
            if (section.hasErrors()) {
                return true;
            }
        }

        return false;
    }

    public String getResultText() {
        var writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        for (var section : sections) {
            section.printListing(out);
        }
        out.close();
        return writer.toString();
    }

    public Map<Integer, Integer> getSourceMap(String sourceName) {
        if (sourceMap.containsKey(sourceName)) {
            return sourceMap.get(sourceName);
        } else {
            return new HashMap<>();
        }
    }
    
    public Map<Integer, Integer> getAddressMap() {
        if (addressMap == null) {
            addressMap = new HashMap<>();
            lineMap = new HashMap<>();
            
            var lineNumber = 0;
            for (var section : sections) {
                lineNumber = section.fillAddressMap(addressMap, lineMap, lineNumber);
            }
        }
        
        return addressMap;
    }
    
    public Map<Integer, Integer> getLineMap() {
        if (lineMap == null) {
            getAddressMap();
        }
        
        return lineMap;
    }

    public static Result errors(ErrorLog log) {
        return new Result() {
            @Override
            public boolean hasErrors() {
                return true;
            }
        };
    }
}
