package edu.depauw.dep10.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UByteTest {
    @Test
    public void withJoinsHighAndLowBytes() {
        assertEquals(0x1234, UByte.of(0x12).with(UByte.of(0x34)).value());
        assertEquals(0xFFFF, UByte.of(0xFF).with(UByte.of(0xFF)).value());
        assertEquals(0x0001, UByte.of(0x00).with(UByte.of(0x01)).value());
        assertEquals(0x8000, UByte.of(0x80).with(UByte.of(0x00)).value());
    }

    @Test
    public void withIsTheInverseOfHiAndLo() {
        for (int v : new int[] { 0x0000, 0x00FF, 0x0100, 0xABCD, 0xFFFF }) {
            var w = Word.of(v);
            assertEquals(v, w.hi().with(w.lo()).value());
        }
    }
}
