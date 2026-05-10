package edu.depauw.declan;

import edu.depauw.declan.ast.ConstInfo;
import edu.depauw.declan.ast.Expr;
import edu.depauw.declan.ast.Scope;
import edu.depauw.declan.ast.VarInfo;
import edu.depauw.declan.ast.Expr.Binary;
import edu.depauw.declan.ast.Expr.Literal;
import edu.depauw.declan.ast.Expr.Unary;
import edu.depauw.declan.ast.Expr.Variable;

public class ConstEvaluator implements Expr.Visitor<Object> {
    private Scope current;
    private Reporter reporter;

    public ConstEvaluator(Scope current, Reporter reporter) {
        this.current = current;
        this.reporter = reporter;
    }

    public static Object eval(Expr expr, Scope current, Reporter reporter) {
        ConstEvaluator ce = new ConstEvaluator(current, reporter);
        return expr.accept(ce);
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {
        Object left = expr.left.accept(this);
        Object right = expr.right.accept(this);

        switch (expr.operator.type) {
        case PLUS:
            if (isInteger(left) && isInteger(right)) {
                return intValue(left) + intValue(right);
            } else if (isNumeric(left) && isNumeric(right)) {
                return doubleValue(left) + doubleValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be numeric.");
                return null;
            }

        case MINUS:
            if (isInteger(left) && isInteger(right)) {
                return intValue(left) - intValue(right);
            } else if (isNumeric(left) && isNumeric(right)) {
                return doubleValue(left) - doubleValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be numeric.");
                return null;
            }

        case STAR:
            if (isInteger(left) && isInteger(right)) {
                return intValue(left) * intValue(right);
            } else if (isNumeric(left) && isNumeric(right)) {
                return doubleValue(left) * doubleValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be numeric.");
                return null;
            }

        case SLASH:
            if (isNumeric(left) && isNumeric(right)) {
                return doubleValue(left) / doubleValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be numeric.");
                return null;
            }

        case DIV:
            if (isInteger(left) && isInteger(right)) {
                return intValue(left) / intValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be integral.");
                return null;
            }

        case MOD:
            if (isInteger(left) && isInteger(right)) {
                return intValue(left) % intValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be integral.");
                return null;
            }

        case AND:
            if (isBoolean(left) && isBoolean(right)) {
                return booleanValue(left) && booleanValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be boolean.");
                return null;
            }

        case OR:
            if (isBoolean(left) && isBoolean(right)) {
                return booleanValue(left) || booleanValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be boolean.");
                return null;
            }

        case EQUAL:
            if (isInteger(left) && isInteger(right)) {
                return intValue(left) == intValue(right);
            } else if (isNumeric(left) && isNumeric(right)) {
                return doubleValue(left) == doubleValue(right);
            } else if (isBoolean(left) && isBoolean(right)) {
                return booleanValue(left) == booleanValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be both numeric or both boolean.");
                return null;
            }

        case NOT_EQUAL:
            if (isInteger(left) && isInteger(right)) {
                return intValue(left) != intValue(right);
            } else if (isNumeric(left) && isNumeric(right)) {
                return doubleValue(left) != doubleValue(right);
            } else if (isBoolean(left) && isBoolean(right)) {
                return booleanValue(left) != booleanValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be both numeric or both boolean.");
                return null;
            }

        case LESS:
            if (isInteger(left) && isInteger(right)) {
                return intValue(left) < intValue(right);
            } else if (isNumeric(left) && isNumeric(right)) {
                return doubleValue(left) < doubleValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be numeric.");
                return null;
            }

        case GREATER:
            if (isInteger(left) && isInteger(right)) {
                return intValue(left) > intValue(right);
            } else if (isNumeric(left) && isNumeric(right)) {
                return doubleValue(left) > doubleValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be numeric.");
                return null;
            }

        case LESS_EQUAL:
            if (isInteger(left) && isInteger(right)) {
                return intValue(left) <= intValue(right);
            } else if (isNumeric(left) && isNumeric(right)) {
                return doubleValue(left) <= doubleValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be numeric.");
                return null;
            }

        case GREATER_EQUAL:
            if (isInteger(left) && isInteger(right)) {
                return intValue(left) >= intValue(right);
            } else if (isNumeric(left) && isNumeric(right)) {
                return doubleValue(left) >= doubleValue(right);
            } else {
                reporter.error(expr.operator.line, "Operands must be numeric.");
                return null;
            }

        default:
            // This should not happen.
            return null;
        }
    }

    private double doubleValue(Object x) {
        if (isInteger(x)) {
            return (int) x;
        } else {
            return (double) x;
        }
    }

    private int intValue(Object x) {
        return (int) x;
    }

    private boolean booleanValue(Object x) {
        return (boolean) x;
    }

    private boolean isInteger(Object x) {
        return x instanceof Integer;
    }

    private boolean isNumeric(Object x) {
        return x instanceof Number;
    }

    private boolean isBoolean(Object x) {
        return x instanceof Boolean;
    }

    @Override
    public Object visitLiteralExpr(Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Unary expr) {
        Object right = expr.right.accept(this);

        switch (expr.operator.type) {
        case PLUS:
            if (isNumeric(right)) {
                return right;
            } else {
                reporter.error(expr.operator.line, "Operand must be numeric.");
            }

        case MINUS:
            if (isInteger(right)) {
                return -intValue(right);
            } else if (isNumeric(right)) {
                return -doubleValue(right);
            } else {
                reporter.error(expr.operator.line, "Operand must be numeric.");
                return null;
            }

        case NOT:
            if (isBoolean(right)) {
                return !booleanValue(right);
            } else {
                reporter.error(expr.operator.line, "Operand must be boolean.");
                return null;
            }

        default:
            // This should not happen.
            return null;
        }
    }

    @Override
    public Object visitVariableExpr(Variable expr) {
        String name = expr.name.lexeme;

        VarInfo info = current.lookup(name);
        if (info != null && info.isConstant()) {
            ConstInfo constInfo = (ConstInfo) info;
            return constInfo.value;
        } else {
            reporter.error(expr.name.line, "Unknown constant '" + name + "'.");
            return null;
        }
    }
}
