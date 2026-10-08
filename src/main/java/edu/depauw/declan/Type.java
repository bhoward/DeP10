package edu.depauw.declan;

/**
 * A Declan type. The four scalar types are shared constants, so they can be
 * compared with ==. Structured types (arrays, records) will be subclasses that
 * report their own {@link #kind()} and {@link #width()}; code that selects on the
 * kind of a type should switch on {@code type.kind()}.
 */
public class Type {
    public enum Kind {
        BOOLEAN, INTEGER, REAL, LONGINT, ARRAY
    }

    public static final Type BOOLEAN = new Type(Kind.BOOLEAN);
    public static final Type INTEGER = new Type(Kind.INTEGER);
    public static final Type REAL = new Type(Kind.REAL);
    public static final Type LONGINT = new Type(Kind.LONGINT);

    private final Kind kind;

    protected Type(Kind kind) {
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /**
     * Number of 16-bit words a variable of this type occupies, both as a global
     * and as a local (stack) slot. Scalars take 1 word, except LONGINT, which is
     * a 32-bit value and takes 2. Slot allocation in Scope/VarInfo and the
     * Pep10/Pep10X code generation must agree with this value.
     */
    public int width() {
        return kind == Kind.LONGINT ? 2 : 1;
    }

    @Override
    public String toString() {
        return kind.toString();
    }
}
