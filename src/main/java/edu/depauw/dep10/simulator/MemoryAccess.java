package edu.depauw.dep10.simulator;

import java.util.HexFormat;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public interface MemoryAccess {
    public record RB(Word addr, UByte value) implements MemoryAccess {
        @Override
        public String toString() {
            var format = HexFormat.of().withUpperCase();

            return format.toHexDigits(addr.value(), 4) + ">" + format.toHexDigits(value.value(), 2);
        }
    }
    
    public record WB(Word addr, UByte value) implements MemoryAccess {
        @Override
        public String toString() {
            var format = HexFormat.of().withUpperCase();

            return format.toHexDigits(addr.value(), 4) + "<" + format.toHexDigits(value.value(), 2);
        }
    }
}
