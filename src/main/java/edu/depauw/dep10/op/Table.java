package edu.depauw.dep10.op;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import edu.depauw.dep10.simulator.Controller;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Pair;
import edu.depauw.dep10.util.Word;

public class Table {
    private TableEntry[] ops;
    private Map<Pair<String, String>, Integer> codes;

    public Table() {
        this.ops = new TableEntry[256];
        this.codes = new HashMap<>();

        Arrays.fill(ops, Operation.UNIMPLEMENTED);
    }

    public void install(int code, Operation op) {
        ops[code] = op;
        codes.put(new Pair<>(op.getMnemonic().toLowerCase(), ""), code);
    }

    public void install(int code, OpCore op) {
        for (Mode mode : op.getModes()) {
            var index = code + mode.getOffset();
            ops[index] = new Operation.NonUnary(op, mode);
            codes.put(new Pair<>(op.getMnemonic().toLowerCase(), mode.getSuffix().toLowerCase()), index);
        }
    }
    
    public void install(int code, Table table) {
        ops[code] = new Operation.Prefix(table);
        for (var entry : table.codes.entrySet()) {
            codes.put(entry.getKey(), (entry.getValue() << 8) | code);
        }
    }

    public Operation get(int n) {
        switch (ops[n & 255]) { // Prefix, if any, is in low byte
        case Operation op:
            return op;
        case Operation.Prefix p:
            return p.getTable().get(n >> 8);
        }
    }

    public int lookup(String mnemonic, String mode) {
        return codes.getOrDefault(new Pair<>(mnemonic.toLowerCase(), mode.toLowerCase()), -1);
    }

    public void perform(State s, Word origpc, Controller control) {
        var pc = s.getPC();
        var opcode = s.mem1(pc);
        s.setOpCode(opcode);
        s.setPC(pc.plus(1));
        
        ops[opcode.value()].perform(s, origpc, control);
    }
}
