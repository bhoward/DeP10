package edu.depauw.declan.ast;

import edu.depauw.declan.Token;
import edu.depauw.declan.Type;

public class Param {
    public interface Visitor<R> {
        R visitParam(Param param);
    }

    public Param(boolean isVar, Token name, Type type) {
        this.isVar = isVar;
        this.name = name;
        this.type = type;
    }

    public final boolean isVar;
    public final Token name;
    public final Type type;
}
