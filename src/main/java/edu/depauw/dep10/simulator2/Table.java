package edu.depauw.dep10.simulator2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;

import edu.depauw.dep10.util.Pair;
import edu.depauw.dep10.util.UByte;

/**
 * Represents a collection of single-byte machine instructions, searchable by
 * opcode byte and by mnemonic.
 */
public class Table {
    private Operation[] ops;
    private Map<Pair<String, String>, Integer> codes;

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
        if (code == 0) {
            throw new IllegalArgumentException("Zero opcodes are not supported");
        }
        ops[code] = op;
        codes.put(new Pair<>(op.getMnemonic().toLowerCase(), mode.toLowerCase()), code);
    }

    /**
     * Add the given {@link Operation} to the table at the given opcode, without
     * registering its mnemonic for {@link #lookup}. This is for entries that are
     * not instructions by themselves, such as the prefix of a two-byte
     * instruction (see {@link edu.depauw.dep10.simulator2.pep10.Pep10Table#installPrefix}).
     * 
     * @param code
     * @param op
     */
    public void install(int code, Operation op) {
        if (code == 0) {
            throw new IllegalArgumentException("Zero opcodes are not supported");
        }
        ops[code] = op;
    }

    /**
     * Copy the mnemonic lookup entries of another table into this one, changing
     * each code with the given function. This lets an extension table reached
     * through a prefix be searched by mnemonic from the main table.
     * 
     * @param other
     * @param remap
     */
    public void installCodes(Table other, IntUnaryOperator remap) {
        for (var entry : other.codes.entrySet()) {
            codes.put(entry.getKey(), remap.applyAsInt(entry.getValue()));
        }
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
}
