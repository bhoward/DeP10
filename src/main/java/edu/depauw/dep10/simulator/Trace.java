package edu.depauw.dep10.simulator;

import java.util.HexFormat;
import java.util.List;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public record Trace(Word a, Word x, Word sp, UByte flags, List<MemoryAccess> accesses) {
    @Override
    public String toString() {
        var result = new StringBuilder();
        var format = HexFormat.of().withUpperCase();
        
        result.append(" A=");
        result.append(format.toHexDigits(a.value(), 4));
        result.append(" X=");
        result.append(format.toHexDigits(x.value(), 4));
        result.append(" SP=");
        result.append(format.toHexDigits(sp.value(), 4));
        result.append(" F=");
        result.append(flags.bit(3) ? "N" : "-");
        result.append(flags.bit(2) ? "Z" : "-");
        result.append(flags.bit(1) ? "V" : "-");
        result.append(flags.bit(0) ? "C" : "-");
        
        for (var access : accesses) {
            result.append(" ");
            result.append(access);
        }
        
        return result.toString();
    }
}
