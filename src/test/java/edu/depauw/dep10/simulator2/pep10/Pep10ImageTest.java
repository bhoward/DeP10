package edu.depauw.dep10.simulator2.pep10;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.depauw.dep10.simulator2.Permissions;
import edu.depauw.dep10.simulator2.common.PlainMemoryDevice;
import edu.depauw.dep10.simulator2.pep10.Pep10Image.SegmentInfo;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class Pep10ImageTest {
    private static Pep10Image image(String object) {
        var image = new Pep10Image();
        image.addString(object);
        return image;
    }

    private static int at(PlainMemoryDevice memory, int address) throws Exception {
        return memory.readB(Word.of(address)).value();
    }

    private static PlainMemoryDevice load(Pep10Image image) {
        var memory = new PlainMemoryDevice(65536);
        for (int i = 0; i < memory.size(); i++) {
            memory.loadB(Word.of(i), UByte.of(0));
        }
        image.loadInto(memory);
        return memory;
    }

    @Test
    public void bytesLoadAtTheirAddresses() throws Exception {
        var memory = load(image("01 02 03\n[0100]\nAA BB\n[0200]\nCC"));
        assertEquals(0x01, at(memory, 0));
        assertEquals(0x03, at(memory, 2));
        assertEquals(0xAA, at(memory, 0x100));
        assertEquals(0xBB, at(memory, 0x101));
        assertEquals(0xCC, at(memory, 0x200));
    }

    @Test
    public void segmentsWithoutFlagsAllowEverything() {
        var segments = image("01 02\n[0100]\nAA").getSegments();
        assertEquals(List.of(new SegmentInfo(0, 2, Permissions.ALL), new SegmentInfo(0x100, 1, Permissions.ALL)),
                segments);
    }

    @Test
    public void flagsAfterTheAddressGiveThePermissions() {
        var segments = image("[0100 rx]\n01 02\n[0200 rw]\n03\n[0300 r]\n04").getSegments();
        assertEquals(3, segments.size());
        assertEquals(new SegmentInfo(0x100, 2, Permissions.parse("rx")), segments.get(0));
        assertEquals(new SegmentInfo(0x200, 1, Permissions.parse("rw")), segments.get(1));
        assertEquals(new SegmentInfo(0x300, 1, Permissions.parse("r")), segments.get(2));
    }

    @Test
    public void flagsMayTouchTheAddress() {
        var segments = image("[0100rw]\n01").getSegments();
        assertEquals(List.of(new SegmentInfo(0x100, 1, Permissions.parse("rw"))), segments);
    }

    @Test
    public void flagsMayBeSeparatedFromTheBracket() {
        var segments = image("[0100 r w ]\n01").getSegments();
        assertEquals(List.of(new SegmentInfo(0x100, 1, Permissions.parse("rw"))), segments);
    }

    @Test
    public void otherLettersAreIgnoredAndNoLettersMeansNoAccess() {
        var segments = image("[0100 rwz]\n01\n[0200 z]\n02").getSegments();
        assertEquals(Permissions.parse("rw"), segments.get(0).permissions());
        assertEquals(Permissions.NONE, segments.get(1).permissions());
    }

    @Test
    public void bytesAfterFlagsStillLoad() throws Exception {
        var memory = load(image("[0100 rx]\nAA BB\n[0200 rw]\nCC"));
        assertEquals(0xAA, at(memory, 0x100));
        assertEquals(0xBB, at(memory, 0x101));
        assertEquals(0xCC, at(memory, 0x200));
    }

    @Test
    public void anEmptyFirstSegmentIsLeftOut() {
        var segments = image("[0100 rw]\n01").getSegments();
        assertEquals(1, segments.size());
        assertEquals(0x100, segments.get(0).start());
    }

    @Test
    public void protectionBitsComeFromTheSegmentFlags() {
        var segments = image("[0100 rx]\n01\n[0200 rw]\n02\n[0300 r]\n03").getSegments();
        assertEquals(1, segments.get(0).permissions().protectionBits());
        assertEquals(3, segments.get(1).permissions().protectionBits());
        assertEquals(2, segments.get(2).permissions().protectionBits());
    }
}
