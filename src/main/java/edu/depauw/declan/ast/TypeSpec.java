package edu.depauw.declan.ast;

import edu.depauw.declan.Token;
import edu.depauw.declan.Type;

/**
 * A type as written in the source: a basic type keyword, the name of a type
 * declared with TYPE, or an ARRAY. The type checker resolves it to a {@link Type}.
 */
public abstract class TypeSpec {
    public interface Visitor<R> {
        R visitBasic(Basic spec);

        R visitNamed(Named spec);

        R visitArray(Array spec);
    }

    public static class Basic extends TypeSpec {
        public final Type type;

        public Basic(Type type) {
            this.type = type;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBasic(this);
        }

        @Override
        public String toString() {
            return type.toString();
        }
    }

    public static class Named extends TypeSpec {
        public final Token name;

        public Named(Token name) {
            this.name = name;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitNamed(this);
        }

        @Override
        public String toString() {
            return name.lexeme;
        }
    }

    public static class Array extends TypeSpec {
        public final Token head;
        public final Expr length;
        public final TypeSpec element;

        public Array(Token head, Expr length, TypeSpec element) {
            this.head = head;
            this.length = length;
            this.element = element;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitArray(this);
        }

        @Override
        public String toString() {
            return "ARRAY " + length + " OF " + element;
        }
    }

    public abstract <R> R accept(Visitor<R> visitor);
}
