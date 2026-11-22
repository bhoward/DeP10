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
import edu.depauw.dep10.simulator2.Permissions;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * An {@link Image} that consists of segments of contiguous bytes, each to be
 * loaded from a given starting address. Segments may be added using the Pep/10
 * object file format: a sequence of whitespace-separated hexadecimal bytes.
 * Each successive segment starts with "[addr]" or "[addr flags]", where "addr"
 * is a four-digit hexadecimal starting address and the optional "flags" are the
 * letters of a .SECTION directive (for example "[0100 rw]" or "[0400 rx]"). The
 * flags give the segment its {@link Permissions}; a segment without flags has
 * {@link Permissions#ALL}. The first segment starts at 0000 if not otherwise
 * specified.
 */
public class Pep10Image implements Image {
    /**
     * A description of one segment of an image: where it starts, how many bytes it
     * holds, and what user mode may do with it.
     * 
     * @param start
     * @param length
     * @param permissions
     */
    public record SegmentInfo(int start, int length, Permissions permissions) {
    }

    private static class Segment {
        private int start;
        private Permissions permissions;
        private List<Integer> contents;

        public Segment(int start, Permissions permissions) {
            this.start = start;
            this.permissions = permissions;
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

    /**
     * @return the segments that hold at least one byte, in the order they were
     *         added, so that a memory device with protection can build its table
     */
    public List<SegmentInfo> getSegments() {
        var result = new ArrayList<SegmentInfo>();
        for (var segment : segments) {
            if (!segment.contents.isEmpty()) {
                result.add(new SegmentInfo(segment.start, segment.contents.size(), segment.permissions));
            }
        }
        return result;
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
        var segment = new Segment(addr, Permissions.ALL);

        while (scanner.hasNext()) {
            String token = scanner.next();
            if (token.startsWith("[")) {
                segments.add(segment);

                addr = Integer.parseInt(token.substring(1, 5), 16);

                // Whatever follows the address, up to the closing bracket, is the flags
                var inside = new StringBuilder(token.substring(5));
                while (inside.indexOf("]") < 0 && scanner.hasNext()) {
                    inside.append(scanner.next());
                }
                var flags = inside.toString().replace("]", "").trim();
                var permissions = flags.isEmpty() ? Permissions.ALL : Permissions.parse(flags);

                segment = new Segment(addr, permissions);
            } else {
                var value = Integer.parseInt(token, 16);
                segment.append(value);
            }
        }
        segments.add(segment);
    }
}
