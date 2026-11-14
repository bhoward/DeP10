package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.Pair;
import edu.depauw.dep10.util.UByte;

public class Table {
    private Operation[] ops;
    
    public Table() {
        ops = new Operation[256];
    }

	public Operation getOp(UByte opCode) throws Fault {
		// TODO Auto-generated method stub
		return null;
	}

    public void install(int code, Operation op) {
        if (code == 0) {
            throw new IllegalArgumentException("Zero opcodes are not supported");
        }
        ops[code] = op;
//        codes.put(new Pair<>(op.getMnemonic().toLowerCase(), ""), code);
    }
}
