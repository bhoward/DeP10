package edu.depauw.dep10.simulator2;

/**
 * What user mode may do with a range of memory: read it, write it, or execute
 * code from it. These are the same flags as the assembler's .SECTION directive
 * (r, w and x), so an image can say which of its segments are code, data or
 * read-only, and a memory device with protection can build its table from them.
 * 
 * @param read
 * @param write
 * @param execute
 */
public record Permissions(boolean read, boolean write, boolean execute) {
    /** Any access, which is what a segment gets when its header gives no flags. */
    public static final Permissions ALL = new Permissions(true, true, true);

    /** No user access. */
    public static final Permissions NONE = new Permissions(false, false, false);

    /**
     * Read the flags of a .SECTION directive: the letters r, w and x, in any order
     * and either case. Other letters (such as z) do not affect protection and are
     * ignored; flags with no r, w or x mean no user access.
     * 
     * @param flags
     * @return the permissions
     */
    public static Permissions parse(String flags) {
        var f = flags == null ? "" : flags.toLowerCase();
        return new Permissions(f.indexOf('r') >= 0, f.indexOf('w') >= 0, f.indexOf('x') >= 0);
    }

    /**
     * The two protection bits (read, write) as the memory protection table stores
     * them: 0 (00) no user access, 2 (10) read only, 3 (11) read and write, and 1
     * (01) read and execute, which is how a code page is marked. Execute makes the
     * segment code, even with w (a page cannot be both writable and executable);
     * r or w alone means data. This is the same mapping as
     * {@code Section.protectionBits} in the assembler.
     * 
     * @return a number from 0 to 3
     */
    public int protectionBits() {
        if (execute) {
            return 1;
        } else if (write) {
            return 3;
        } else if (read) {
            return 2;
        }
        return 0;
    }

    @Override
    public String toString() {
        return (read ? "r" : "-") + (write ? "w" : "-") + (execute ? "x" : "-");
    }
}
