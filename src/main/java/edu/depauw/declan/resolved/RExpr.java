package edu.depauw.declan.resolved;

import edu.depauw.declan.TokenType;
import edu.depauw.declan.Type;

public abstract class RExpr {
    public Type type;

    public interface Visitor<R> {
        R visitArrayBase(ArrayBase expr);

        R visitBinary(Binary expr);

        R visitElement(Element expr);

        R visitElementAddr(ElementAddr expr);

        R visitLiteral(Literal expr);

        R visitUnary(Unary expr);

        R visitVariable(Variable expr);
    }

    public static class Binary extends RExpr {
        public static enum OpCode {
            IADD, FADD, ISUB, FSUB, IMUL, FMUL, IDIV, FDIV, IMOD, LAND, LOR, IEQ, FEQ, LEQ, ILT, FLT, LADD, LSUB, LMUL, LDIV,
            LMOD, LEQL, LLT
        }

        public final OpCode op;
        public final RExpr left;
        public final RExpr right;

        public Binary(Type type, OpCode op, RExpr left, RExpr right) {
            this.type = type;
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinary(this);
        }

        @Override
        public String toString() {
            return String.format("(%s %s %s)", left, op, right);
        }
    }

    public static class Unary extends RExpr {
        public static enum OpCode {
            INEG, FNEG, LNOT, FLOAT, LONG, LNEG, REF
        }

        public final OpCode op;
        public final RExpr right;

        public Unary(Type type, OpCode op, RExpr right) {
            this.type = type;
            this.op = op;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnary(this);
        }

        @Override
        public String toString() {
            return String.format("(%s %s)", op, right);
        }
    }

    public static class Literal extends RExpr {
        public final Object value;

        public Literal(Type type, Object value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteral(this);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    /** Evaluates to the address of the first element of an array variable. */
    public static class ArrayBase extends RExpr {
        public final Location loc;

        public ArrayBase(Type arrayType, Location loc) {
            this.type = arrayType;
            this.loc = loc;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitArrayBase(this);
        }

        @Override
        public String toString() {
            return "&" + loc;
        }
    }

    /**
     * Evaluates to the address of base[index]; its type is the element type. The
     * index is checked against the length of the array, which is the type of base.
     */
    public static class ElementAddr extends RExpr {
        public final RExpr base;
        public final RExpr index;

        public ElementAddr(Type elementType, RExpr base, RExpr index) {
            this.type = elementType;
            this.base = base;
            this.index = index;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitElementAddr(this);
        }

        @Override
        public String toString() {
            return String.format("&%s[%s]", base, index);
        }
    }

    /** The value stored at an address; its type is the type of the value. */
    public static class Element extends RExpr {
        public final RExpr addr;

        public Element(RExpr addr) {
            this.type = addr.type;
            this.addr = addr;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitElement(this);
        }

        @Override
        public String toString() {
            return "*" + addr;
        }
    }

    public static class Variable extends RExpr {
        public final Location loc;

        public Variable(Type type, Location loc) {
            this.type = type;
            this.loc = loc;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariable(this);
        }

        @Override
        public String toString() {
            return loc.toString();
        }
    }

    public static RExpr makeBinary(Type type, TokenType operator, RExpr left, RExpr right) {
        switch (operator) {
        case AND:
            return new Binary(type, Binary.OpCode.LAND, left, right);
        case DIV:
            if (left.type == Type.LONGINT) {
                return new Binary(type, Binary.OpCode.LDIV, left, right);
            }
            return new Binary(type, Binary.OpCode.IDIV, left, right);
        case EQUAL:
            if (left.type == Type.LONGINT) {
                return new Binary(type, Binary.OpCode.LEQL, left, right);
            } else if (left.type == Type.BOOLEAN) {
                return new Binary(type, Binary.OpCode.LEQ, left, right);
            } else if (left.type == Type.INTEGER) {
                return new Binary(type, Binary.OpCode.IEQ, left, right);
            } else {
                return new Binary(type, Binary.OpCode.FEQ, left, right);
            }
        case GREATER:
            return makeBinary(type, TokenType.LESS, right, left);
        case GREATER_EQUAL:
            return makeUnary(type, TokenType.NOT, makeBinary(type, TokenType.LESS, left, right));
        case LESS:
            if (left.type == Type.LONGINT) {
                return new Binary(type, Binary.OpCode.LLT, left, right);
            } else if (left.type == Type.INTEGER) {
                return new Binary(type, Binary.OpCode.ILT, left, right);
            } else {
                return new Binary(type, Binary.OpCode.FLT, left, right);
            }
        case LESS_EQUAL:
            return makeUnary(type, TokenType.NOT, makeBinary(type, TokenType.LESS, right, left));
        case MINUS:
            if (left.type == Type.LONGINT) {
                return new Binary(type, Binary.OpCode.LSUB, left, right);
            } else if (left.type == Type.INTEGER) {
                return new Binary(type, Binary.OpCode.ISUB, left, right);
            } else {
                return new Binary(type, Binary.OpCode.FSUB, left, right);
            }
        case MOD:
            if (left.type == Type.LONGINT) {
                return new Binary(type, Binary.OpCode.LMOD, left, right);
            }
            return new Binary(type, Binary.OpCode.IMOD, left, right);
        case NOT_EQUAL:
            return makeUnary(type, TokenType.NOT, makeBinary(type, TokenType.EQUAL, left, right));
        case OR:
            return new Binary(type, Binary.OpCode.LOR, left, right);
        case PLUS:
            if (left.type == Type.LONGINT) {
                return new Binary(type, Binary.OpCode.LADD, left, right);
            } else if (left.type == Type.INTEGER) {
                return new Binary(type, Binary.OpCode.IADD, left, right);
            } else {
                return new Binary(type, Binary.OpCode.FADD, left, right);
            }
        case SLASH:
            return new Binary(type, Binary.OpCode.FDIV, left, right);
        case STAR:
            if (left.type == Type.LONGINT) {
                return new Binary(type, Binary.OpCode.LMUL, left, right);
            } else if (left.type == Type.INTEGER) {
                return new Binary(type, Binary.OpCode.IMUL, left, right);
            } else {
                return new Binary(type, Binary.OpCode.FMUL, left, right);
            }
        default:
            // This should not happen
            return null;
        }
    }

    public static RExpr makeUnary(Type type, TokenType operator, RExpr right) {
        switch (operator) {
        case MINUS:
            if (right.type == Type.LONGINT) {
                return new Unary(type, Unary.OpCode.LNEG, right);
            } else if (right.type == Type.INTEGER) {
                return new Unary(type, Unary.OpCode.INEG, right);
            } else {
                return new Unary(type, Unary.OpCode.FNEG, right);
            }
        case NOT:
            return new Unary(type, Unary.OpCode.LNOT, right);
        case PLUS:
            return right;
        default:
            // This should not happen
            return null;
        }
    }

    public static RExpr makeCast(Type type, RExpr expr) {
        if (type == Type.LONGINT) {
            return new Unary(type, Unary.OpCode.LONG, expr);
        }
        return new Unary(type, Unary.OpCode.FLOAT, expr);
    }

    public static RExpr makeLiteral(Type type, Object value) {
        return new Literal(type, value);
    }

    public static RExpr makeVariable(Type type, Location loc) {
        return new Variable(type, loc);
    }

    public static RExpr makeArrayBase(Type arrayType, Location loc) {
        return new ArrayBase(arrayType, loc);
    }

    public static RExpr makeElementAddr(Type elementType, RExpr base, RExpr index) {
        return new ElementAddr(elementType, base, index);
    }

    public static RExpr makeElement(RExpr addr) {
        return new Element(addr);
    }

    public static RExpr makeRef(RExpr expr) {
        return new Unary(expr.type, Unary.OpCode.REF, expr);
    }

    public abstract <R> R accept(Visitor<R> visitor);
}
