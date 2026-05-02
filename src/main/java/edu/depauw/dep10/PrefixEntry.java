package edu.depauw.dep10;

import edu.depauw.dep10.util.UByte;

public record PrefixEntry(OpTable table) implements OpTableEntry {
    public OpTableEntry get(UByte opcode) {
        return table.get(opcode);
    }
}
