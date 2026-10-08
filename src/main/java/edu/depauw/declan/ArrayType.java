package edu.depauw.declan;

/**
 * ARRAY length OF element. Elements are numbered from 0 to length - 1 and are
 * stored one after another, so the array occupies length * element.width()
 * words.
 */
public class ArrayType extends Type {
    public final int length;
    public final Type element;

    public ArrayType(int length, Type element) {
        super(Kind.ARRAY);
        this.length = length;
        this.element = element;
    }

    @Override
    public int width() {
        return length * element.width();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArrayType a && a.length == length && a.element.equals(element);
    }

    @Override
    public int hashCode() {
        return 31 * length + element.hashCode();
    }

    @Override
    public String toString() {
        return "ARRAY " + length + " OF " + element;
    }
}
