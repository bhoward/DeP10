package edu.depauw.dep10.simulator2.dep10;

import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.Operation;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.simulator2.Table;

/**
 * Specialization of {@link Table} where some entries can be prefix opcodes that
 * will be followed by another byte giving an opcode in an extension table.
 */
public class ExtensibleTable extends Table {
    public ExtensibleTable(Operation undefined) {
        super(undefined);
    }

    /**
     * Install an extension table of two-byte instructions behind the given prefix
     * opcode. Looking up a mnemonic installed in the extension table returns the
     * prefix in the high byte and the second opcode in the low byte, in the same
     * order as the bytes in memory. A prefix must not be zero.
     * 
     * @param prefix the opcode of the prefix
     * @param ext    the table for the byte that follows the prefix
     */
    public void installPrefix(int prefix, Table ext) {
        if (prefix == 0) {
            throw new IllegalArgumentException("Zero prefixes are not supported");
        }

        ops[prefix] = new Prefix(ext);
        for (var entry : ext.getCodes()) {
            codes.put(entry.getKey(), (prefix << 8) | entry.getValue());
        }
    }

    /**
     * The entry in a {@link Table} for a prefix opcode: it is not itself a complete
     * instruction, but names the table where the next byte is looked up. Machines
     * that extend Pep/10 with two-byte instructions, such as the DeP10 machine,
     * read the second byte in their fetch and look it up in {@link #getTable()}.
     */
    public static final class Prefix implements Operation {
        private final Table table;

        private Prefix(Table table) {
            this.table = table;
        }

        /**
         * @return the table for the byte that follows the prefix
         */
        public Table getTable() {
            return table;
        }

        @Override
        public void execute(State state) throws Fault {
            throw new Fault("Incomplete instruction");
        }

        @Override
        public String getMnemonic() {
            return "PREFIX";
        }

        @Override
        public boolean hasOperand() {
            return false;
        }
    }
}
