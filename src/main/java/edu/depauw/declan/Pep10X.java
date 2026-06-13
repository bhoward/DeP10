package edu.depauw.declan;

import edu.depauw.declan.ir.Instruction.Nullary;

public class Pep10X extends Pep10 {
    @Override
    public void writeRuntime() {
        out("ReadInt: @DECI 2,sf");
        out("       RET");
        out("WriteInt: LDBA ' ',i");
        out("       STBA charOut,d");
        out("       @DECO 2,s");
        out("       RET");
        out("WriteLn: LDBA '\\n',i");
        out("       STBA charOut,d");
        out("       RET");
    }

    @Override
    void translate(Nullary n) {
        switch (n.op) {
        case IDIV:
            out("LDWA 2,s");
            out("DIVA 0,s");
            out("ADDSP 2,i");
            out("STWA 0,s");
            break;
        case IMOD:
            out("LDWA 2,s");
            out("MODA 0,s");
            out("ADDSP 2,i");
            out("STWA 0,s");
            break;
        case IMUL:
            out("LDWA 2,s");
            out("MULA 0,s");
            out("ADDSP 2,i");
            out("STWA 0,s");
            break;
        default:
            super.translate(n);
        }
    }
}
