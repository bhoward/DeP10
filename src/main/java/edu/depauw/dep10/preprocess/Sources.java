package edu.depauw.dep10.preprocess;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
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
        deque.add(new StdInSource());
    }

    public void pushFile(String filename, ErrorLog log) {
        deque.push(new FileSource(filename, log));
    }

    public void pushLines(List<Line> lines) {
        deque.push(new LinesSource(lines));
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

interface Source extends Iterator<Line> {
    // TODO add name and line number tracking
}

class LinesSource implements Source {
    private Iterator<Line> it;

    public LinesSource(List<Line> lines) {
        this.it = lines.iterator();
    }

    public boolean hasNext() {
        return it.hasNext();
    }

    public Line next() {
        return it.next();
    }
}

class StdInSource implements Source {
    private Iterator<Line> it;

    public StdInSource() {
        Reader reader = new InputStreamReader(System.in);
        this.it = Parser.parse(reader).iterator();
    }

    public boolean hasNext() {
        return it.hasNext();
    }

    public Line next() {
        return it.next();
    }
}

class FileSource implements Source {
    private String filename;
    private ErrorLog log;
    private Reader reader;
    private Iterator<Line> it;

    public FileSource(String filename, ErrorLog log) {
        this.filename = filename;
        this.log = log;
        this.reader = null;
    }

    public boolean hasNext() {
        try {
            if (reader == null) {
                reader = new FileReader(filename);
                it = Parser.parse(reader).iterator();
            }

            if (it.hasNext()) {
                return true;
            } else {
                reader.close();
                return false;
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public Line next() {
        return it.next();
    }
}

class ResourceSource implements Source {
    private String resource;
    private ErrorLog log;
    private Reader reader;
    private Iterator<Line> it;

    public ResourceSource(String resource, ErrorLog log) {
        this.resource = resource;
        this.log = log;
        this.reader = null;
    }

    public boolean hasNext() {
        try {
            if (reader == null) {
                URL url = getClass().getResource(resource);
                if (url == null) {
                    log.error("Unable to open resource " + resource);
                    return false;
                }
                
                reader = new InputStreamReader(url.openStream());
                it = Parser.parse(reader).iterator();
            }

            if (it.hasNext()) {
                return true;
            } else {
                reader.close();
                return false;
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public Line next() {
        return it.next();
    }
}
