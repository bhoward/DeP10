package edu.depauw.dep10.preprocess;

import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import edu.depauw.dep10.Parser;
import edu.depauw.dep10.driver.ErrorLog;

/**
 * Maintain a queue of open and pending sources: external files, resources,
 * standard input, or expanded macros, and present a uniform interface of an
 * iterator of Lines.
 */
public class Sources implements Iterator<Line> {
    private Deque<Source> deque = new ArrayDeque<>();

    public boolean hasNext() {
        while (!deque.isEmpty() && !deque.peek().hasNext()) {
            deque.pop();
        }

        return !deque.isEmpty();
    }

    public Line next() {
        // Precondition: hasNext() is true
        return deque.peek().next();
    }

    public void addFile(String filename, ErrorLog log) {
        deque.add(new FileSource(filename, log));
    }

    public void addResource(String resource, ErrorLog log) {
        deque.add(new ResourceSource(resource, log));
    }

    public void addStdIn() {
        var reader = new InputStreamReader(System.in);
        var it = Parser.parse(reader).iterator();
        var source = new Source("<stdin>", it);

        deque.add(source);
    }

    public void pushFile(String filename, ErrorLog log) {
        deque.push(new FileSource(filename, log));
    }

    public void pushLines(String macroName, List<Line> lines) {
        var it = lines.iterator();
        var source = new Source(macroName, it);

        deque.push(source);
    }

    public List<Line> extractUntil(String end) {
        List<Line> result = new ArrayList<>();

        var it = deque.peek();
        while (it.hasNext()) {
            var line = it.next();
            if (line.command().equalsIgnoreCase(end)) {
                break;
            } else {
                result.add(line);
            }
        }

        return result;
    }
}

// TODO allow some sources (e.g., standard macros or OS) to be "hidden" from listing?
