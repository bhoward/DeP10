package edu.depauw.dep10.simulator2.pep10;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.depauw.dep10.simulator2.Image;
import edu.depauw.dep10.simulator2.MemoryDevice;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * An {@link Image} that consists of segments of contiguous bytes, each to be
 * loaded from a given starting address. Segments may be added using the Pep/10
 * object file format: a sequence of whitespace-separated hexadecimal bytes.
 * Each successive segment starts with "[addr...]", where "addr" is a four-digit
 * hexadecimal starting address (and the rest of the contents of the brackets
 * will be ignored). The first segment starts at 0000 if not otherwise
 * specified.
 */
public class Pep10Image implements Image {
    private static class Segment {
        private int start;
        private List<Integer> contents;

        public Segment(int start) {
            this.start = start;
            this.contents = new ArrayList<>();
        }

        public void append(int value) {
            contents.add(value);
        }

        public void loadInto(MemoryDevice memory) {
            for (int i = 0; i < contents.size(); i++) {
                int address = (start + i) % memory.size();
                memory.loadB(Word.of(address), UByte.of(contents.get(i)));
            }
        }
    }

    private List<Segment> segments;

    public Pep10Image() {
        this.segments = new ArrayList<>();
    }

    @Override
    public void loadInto(MemoryDevice memory) {
        for (var segment : segments) {
            segment.loadInto(memory);
        }
    }

    public void addFile(String filename) {
        try (var scanner = new Scanner(new File(filename))) {
            add(scanner);
        } catch (FileNotFoundException e) {
            // TODO handle this somewhere
            System.err.println("Unable to load " + filename);
        }
    }

    public void addResource(String resource) {
        URL url = getClass().getResource(resource);
        if (url == null) {
            // TODO handle this somewhere
            System.err.println("Unable to open resource " + resource);
        }

        try (var scanner = new Scanner(url.openStream())) {
            add(scanner);
        } catch (IOException e) {
            // TODO handle this somewhere
            System.err.println("Unable to open resource " + resource);
        }
    }

    public void addString(String object) {
        try (var scanner = new Scanner(object)) {
            add(scanner);
        }
    }

    private void add(Scanner scanner) {
        int addr = 0;
        var segment = new Segment(addr);

        while (scanner.hasNext()) {
            String token = scanner.next();
            if (token.startsWith("[")) {
                segments.add(segment);

                addr = Integer.parseInt(token.substring(1, 5), 16);
                segment = new Segment(addr);
            } else {
                var value = Integer.parseInt(token, 16);
                segment.append(value);
            }
        }
        segments.add(segment);
    }
}
