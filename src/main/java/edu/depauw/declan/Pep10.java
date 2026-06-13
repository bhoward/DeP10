package edu.depauw.declan;

import java.util.ArrayList;
import java.util.List;

import edu.depauw.declan.ir.Instruction;
import edu.depauw.declan.ir.Label;
import edu.depauw.declan.ir.Instruction.BinaryString;
import edu.depauw.declan.ir.Instruction.Nullary;
import edu.depauw.declan.ir.Instruction.UnaryInteger;
import edu.depauw.declan.ir.Instruction.UnaryString;

public class Pep10 {
    private List<String> result;
    private int labelSeqNo;

    public Pep10() {
        this.result = new ArrayList<>();
        this.labelSeqNo = 0;
    }

    public List<String> translate(List<Instruction> instructions) {
        for (Instruction instr : instructions) {
            if (instr instanceof Label label) {
                translate(label);
            } else if (instr instanceof Instruction.Nullary n) {
                translate(n);
            } else if (instr instanceof Instruction.UnaryInteger ui) {
                translate(ui);
            } else if (instr instanceof Instruction.UnaryString us) {
                translate(us);
            } else if (instr instanceof Instruction.BinaryString bs) {
                translate(bs);
            } else {
                System.err.println("Unsupported instruction: " + instr);
            }
        }

        writeRuntime();

        return result;
    }

    void writeRuntime() {
        out("ReadInt: @DECI 2,sf");
        out("       RET");
        out("WriteInt: LDBA ' ',i");
        out("       STBA charOut,d");
        out("       @DECO 2,s");
        out("       RET");
        out("WriteLn: LDBA '\\n',i");
        out("       STBA charOut,d");
        out("       RET");
        out("_imul: STWX -4,s");
        out("       LDWX 16,i");
        out("       LDWA 0,i");
        out("       STWA -2,s");
        out("_im0:  LDWA -2,s");
        out("       ASRA");
        out("       STWA -2,s");
        out("       LDWA 2,s");
        out("       RORA");
        out("       STWA 2,s");
        out("       BRC _im2");
        out("_im1:  SUBX 1,i");
        out("       BRNE _im0");
        out("       BR _im4");
        out("_im2:  LDWA -2,s");
        out("       SUBA 4,s");
        out("       STWA -2,s");
        out("_im3:  SUBX 1,i");
        out("       BREQ _im4");
        out("       LDWA -2,s");
        out("       ASRA");
        out("       STWA -2,s");
        out("       LDWA 2,s");
        out("       RORA");
        out("       STWA 2,s");
        out("       BRC _im3");
        out("       LDWA -2,s");
        out("       ADDA 4,s");
        out("       STWA -2,s");
        out("       BR _im1");
        out("_im4:  LDWA -2,s");
        out("       ASRA");
        out("       LDWX 2,s");
        out("       RORX");
        out("       STWX 4,s");
        out("       STWA 2,s");
        out("       LDWX -4,s");
        out("       RET");
        out("_idiv: STWX -6,s");
        out("       LDWX 0,i");
        out("       LDWA 4,s");
        out("       BRGE _id5");
        out("       NEGA");
        out("       STWA 4,s");
        out("       ORX 3,i");
        out("_id5:  LDWA 2,s");
        out("       BRGE _id6");
        out("       NEGA");
        out("       STWA 2,s");
        out("       ADDX 2,i");
        out("_id6:  STBX -3,s");
        out("       LDWA 0,i");
        out("       STWA -2,s");
        out("       LDWX 16,i");
        out("_id7:  LDWA 4,s");
        out("       ASLA");
        out("       STWA 4,s");
        out("       LDWA -2,s");
        out("       ROLA");
        out("       BRC _id8");
        out("       SUBA 2,s");
        out("       BR _id9");
        out("_id8:  ADDA 2,s");
        out("_id9:  STWA -2,s");
        out("       BRLT _id10");
        out("       LDWA 4,s");
        out("       ORA 1,i");
        out("       STWA 4,s");
        out("_id10: SUBX 1,i");
        out("       BRNE _id7");
        out("       LDWA -2,s");
        out("       BRGE _id11");
        out("       ADDA 2,s");
        out("_id11: LDBX -3,s");
        out("       ANDX 1,i");
        out("       BREQ _id12");
        out("       NEGA");
        out("_id12: STWA 2,s");
        out("       LDBX -3,s");
        out("       ANDX 2,i");
        out("       BREQ _id13");
        out("       LDWA 4,s");
        out("       NEGA");
        out("       STWA 4,s");
        out("_id13: LDWX -6,s");
        out("       RET");
    }

    String newLabel() {
        return "__" + labelSeqNo++;
    }

    void out(String s) {
        result.add(s);
    }

    void out(String fmt, Object... objects) {
        result.add(String.format(fmt, objects));
    }

    void translate(Label label) {
        out(label.toString());
    }

    void translate(Nullary n) {
        switch (n.op) {
        case DUP:
            out("LDWA 0,s");
            out("SUBSP 2,i");
            out("STWA 0,s");
            break;
        case END:
            out("RET");
            break;
        case IADD:
            out("LDWA 2,s");
            out("ADDA 0,s");
            out("ADDSP 2,i");
            out("STWA 0,s");
            break;
        case IDIV:
            out("CALL _idiv,i");
            out("ADDSP 2,i");
            break;
        case IEQ: {
            String l1 = newLabel();
            String l2 = newLabel();
            out("LDWA 2,s");
            out("CPWA 0,s");
            out("BREQ %s,i", l1);
            out("LDWA 0,i");
            out("BR %s,i", l2);
            out("%s: LDWA 1,i", l1);
            out("%s: ADDSP 2,i", l2);
            out("STWA 0,s");
            break;
        }
        case ILT: {
            String l1 = newLabel();
            String l2 = newLabel();
            out("LDWA 2,s");
            out("CPWA 0,s");
            out("BRLT %s,i", l1);
            out("LDWA 0,i");
            out("BR %s,i", l2);
            out("%s: LDWA 1,i", l1);
            out("%s: ADDSP 2,i", l2);
            out("STWA 0,s");
            break;
        }
        case IMOD:
            out("CALL _idiv,i");
            out("LDWA 0,s");
            out("STWA 2,s");
            out("ADDSP 2,i");
            break;
        case IMUL:
            out("CALL _imul,i");
            out("ADDSP 2,i");
            break;
        case INEG:
            out("LDWA 0,s");
            out("NEGA");
            out("STWA 0,s");
            break;
        case ISUB:
            out("LDWA 2,s");
            out("SUBA 0,s");
            out("ADDSP 2,i");
            out("STWA 0,s");
            break;
        case LAND:
            out("LDWA 2,s");
            out("ANDA 0,s");
            out("ADDSP 2,i");
            out("STWA 0,s");
            break;
        case LEQ:
            out("LDWA 2,s");
            out("ADDA 0,s");
            out("ADDA 1,i");
            out("ANDA 1,i");
            out("ADDSP 2,i");
            out("STWA 0,s");
            break;
        case LNOT:
            out("LDWA 1,i");
            out("SUBA 0,s");
            out("STWA 0,s");
            break;
        case LOR:
            out("LDWA 2,s");
            out("ORA 0,s");
            out("ADDSP 2,i");
            out("STWA 0,s");
            break;
        case RESTOREFP:
            out("LDWX 0,s");
            out("ADDSP 2,i");
            break;
        case RETURN:
            out("RET");
            break;
        case SAVEFP:
            out("SUBSP 2,i");
            out("STWX 0,s");
            break;
        case SWAP:
            out("LDWA 2,s");
            out("STWA -2,s");
            out("LDWA 0,s");
            out("STWA 2,s");
            out("LDWA -2,s");
            out("STWA 0,s");
            break;
        default:
            System.err.println("Unsupported instruction: " + n);
        }
    }

    void translate(UnaryInteger ui) {
        switch (ui.op) {
        case DROP:
            out("ADDSP %d,i", 2 * ui.value);
            break;
        case ICONST:
            out(".WORD %d", ui.value);
            break;
        case ILD_CONST:
            out("LDWA %d,i", ui.value);
            out("SUBSP 2,i");
            out("STWA 0,s");
            break;
        case ILD_GLOBAL:
            out("LDWA _g%d,d", ui.value);
            out("SUBSP 2,i");
            out("STWA 0,s");
            break;
        case ILD_LOCAL:
            out("LDWA %d,x", -2 * ui.value);
            out("SUBSP 2,i");
            out("STWA 0,s");
            break;
        case ILD_VARP:
            out("LDWA %d,x", -2 * ui.value);
            out("SUBSP 2,i");
            out("STWA 0,s");
            out("LDWA 0,sf");
            out("STWA 0,s");
            break;
        case IRF_GLOBAL:
            out("LDWA _g%d,i", ui.value);
            out("SUBSP 2,i");
            out("STWA 0,s");
            break;
        case IRF_LOCAL:
            out("SUBSP 2,i");
            out("STWX 0,s");
            out("LDWA 0,s");
            out("ADDA %d,i", -2 * ui.value);
            out("STWA 0,s");
            break;
        case IST_GLOBAL:
            out("LDWA 0,s");
            out("ADDSP 2,i");
            out("STWA _g%d,d", ui.value);
            break;
        case IST_LOCAL:
            out("LDWA 0,s");
            out("ADDSP 2,i");
            out("STWA %d,x", -2 * ui.value);
            break;
        case IST_VARP:
            out("LDWA %d,x", -2 * ui.value);
            out("STWA -2,s");
            out("LDWA 0,s");
            out("STWA -2,sf");
            out("ADDSP 2,i");
            break;
        case SETFP:
            out("MOVSPA");
            out("ADDA %d,i", 2 * ui.value - 2);
            out("STWA -2,s");
            out("LDWX -2,s");
            break;
        default:
            System.err.println("Unsupported instruction: " + ui);
        }
    }

    void translate(UnaryString us) {
        switch (us.op) {
        case BRANCH:
            out("BR %s,i", us.value);
            break;
        case BRTRUE:
            out("LDWA 0,s");
            out("ADDSP 2,i");
            out("BRNE %s,i", us.value);
            break;
        case CALL:
            out("CALL %s,i", us.value);
            break;
        default:
            System.err.println("Unsupported instruction: " + us);
        }
    }

    void translate(BinaryString bs) {
        switch (bs.op) {
        case BRIEQ:
            out("LDWA 2,s");
            out("CPWA 0,s");
            out("ADDSP 4,i");
            out("BREQ %s,i", bs.left);
            out("BR %s,i", bs.right);
            break;
        case BRILT:
            out("LDWA 2,s");
            out("CPWA 0,s");
            out("ADDSP 4,i");
            out("BRLT %s,i", bs.left);
            out("BR %s,i", bs.right);
            break;
        default:
            System.err.println("Unsupported instruction: " + bs);
        }
    }
}
