package edu.depauw.dep10.simulator2;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.depauw.dep10.util.Pair;
import edu.depauw.dep10.util.UByte;

/**
 * Represents a collection of single-byte machine instructions, searchable by
 * opcode byte and by mnemonic.
 */
public class Table {
    protected Operation[] ops;
    protected Map<Pair<String, String>, Integer> codes;

    /**
     * Construct an opcode table initially filled with an undefined operation.
     * 
     * @param undefined
     */
    public Table(Operation undefined) {
        this.ops = new Operation[256];
        this.codes = new HashMap<>();

        Arrays.fill(ops, undefined);
    }

    /**
     * Lookup an operation given its opcode.
     * 
     * @param opCode
     * @return the Operation
     */
    public Operation getOp(UByte opCode) {
        return ops[opCode.value()];
    }

    /**
     * Add the given {@link Operation} to the table at the given opcode. Multiple
     * variations of an operation with the same mnemonic may be installed with
     * different modes; if there is only one version, the mode should be "".
     * 
     * @param code
     * @param op
     * @param mode
     */
    public void install(int code, Operation op, String mode) {
        ops[code] = op;
        codes.put(new Pair<>(op.getMnemonic().toLowerCase(), mode.toLowerCase()), code);
    }

    /**
     * Lookup an opcode given the operation's mnemonic and mode (or "" if none).
     * 
     * @param mnemonic
     * @param mode
     * @return the opcode
     */
    public int lookup(String mnemonic, String mode) {
        return codes.getOrDefault(new Pair<>(mnemonic.toLowerCase(), mode.toLowerCase()), -1);
    }
    
    /**
     * @return an unmodifiable set view of the entries in the codes map
     */
    public Set<Entry<Pair<String, String>, Integer>> getCodes() {
        return Collections.unmodifiableSet(codes.entrySet());
    }
}
