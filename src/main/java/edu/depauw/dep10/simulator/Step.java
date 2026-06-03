package edu.depauw.dep10.simulator;

import java.util.HexFormat;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public record Step(Word pc, UByte prefix, UByte opCode, Operation op, Word operand, Word address, Trace trace) {
    @Override
    public String toString() {
        var result = new StringBuilder();
        var format = HexFormat.of().withUpperCase();
        
        result.append(format.toHexDigits(pc.value(), 4));
        result.append(" ");
        result.append(format.toHexDigits(prefix.value(), 2));
        result.append(format.toHexDigits(opCode.value(), 2));
        result.append(" ");
        result.append(op.toString());
        result.append(" ");
        if (op.hasOperand()) {
            result.append(format.toHexDigits(operand.value(), 4));
            result.append(" ");
            result.append(format.toHexDigits(address.value(), 4));
        } else {
            result.append("         ");
        }
        result.append(trace);
        
        return result.toString();
    }
}
