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

    public static List<String> translate(List<Instruction> instructions) {
        Pep10 pep10 = new Pep10();

        for (Instruction instr : instructions) {
            if (instr instanceof Label label) {
                pep10.translate(label);
            } else if (instr instanceof Instruction.Nullary n) {
                pep10.translate(n);
            } else if (instr instanceof Instruction.UnaryInteger ui) {
                pep10.translate(ui);
            } else if (instr instanceof Instruction.UnaryString us) {
                pep10.translate(us);
            } else if (instr instanceof Instruction.BinaryString bs) {
                pep10.translate(bs);
            } else {
                System.err.println("Unsupported instruction: " + instr);
            }
        }

        pep10.out("ReadInt: @DECI 2,sf");
        pep10.out("       RET");
        pep10.out("WriteInt: LDBA ' ',i");
        pep10.out("       STBA charOut,d");
        pep10.out("       @DECO 2,s");
        pep10.out("       RET");
        pep10.out("WriteLn: LDBA '\\n',i");
        pep10.out("       STBA charOut,d");
        pep10.out("       RET");
        pep10.out("_imul: STWX -4,s");
        pep10.out("       LDWX 16,i");
        pep10.out("       LDWA 0,i");
        pep10.out("       STWA -2,s");
        pep10.out("_im0:  LDWA -2,s");
        pep10.out("       ASRA");
        pep10.out("       STWA -2,s");
        pep10.out("       LDWA 2,s");
        pep10.out("       RORA");
        pep10.out("       STWA 2,s");
        pep10.out("       BRC _im2");
        pep10.out("_im1:  SUBX 1,i");
        pep10.out("       BRNE _im0");
        pep10.out("       BR _im4");
        pep10.out("_im2:  LDWA -2,s");
        pep10.out("       SUBA 4,s");
        pep10.out("       STWA -2,s");
        pep10.out("_im3:  SUBX 1,i");
        pep10.out("       BREQ _im4");
        pep10.out("       LDWA -2,s");
        pep10.out("       ASRA");
        pep10.out("       STWA -2,s");
        pep10.out("       LDWA 2,s");
        pep10.out("       RORA");
        pep10.out("       STWA 2,s");
        pep10.out("       BRC _im3");
        pep10.out("       LDWA -2,s");
        pep10.out("       ADDA 4,s");
        pep10.out("       STWA -2,s");
        pep10.out("       BR _im1");
        pep10.out("_im4:  LDWA -2,s");
        pep10.out("       ASRA");
        pep10.out("       LDWX 2,s");
        pep10.out("       RORX");
        pep10.out("       STWX 4,s");
        pep10.out("       STWA 2,s");
        pep10.out("       LDWX -4,s");
        pep10.out("       RET");
        pep10.out("_idiv: STWX -6,s");
        pep10.out("       LDWX 0,i");
        pep10.out("       LDWA 4,s");
        pep10.out("       BRGE _id5");
        pep10.out("       NEGA");
        pep10.out("       STWA 4,s");
        pep10.out("       ORX 3,i");
        pep10.out("_id5:  LDWA 2,s");
        pep10.out("       BRGE _id6");
        pep10.out("       NEGA");
        pep10.out("       STWA 2,s");
        pep10.out("       ADDX 2,i");
        pep10.out("_id6:  STBX -3,s");
        pep10.out("       LDWA 0,i");
        pep10.out("       STWA -2,s");
        pep10.out("       LDWX 16,i");
        pep10.out("_id7:  LDWA 4,s");
        pep10.out("       ASLA");
        pep10.out("       STWA 4,s");
        pep10.out("       LDWA -2,s");
        pep10.out("       ROLA");
        pep10.out("       BRC _id8");
        pep10.out("       SUBA 2,s");
        pep10.out("       BR _id9");
        pep10.out("_id8:  ADDA 2,s");
        pep10.out("_id9:  STWA -2,s");
        pep10.out("       BRLT _id10");
        pep10.out("       LDWA 4,s");
        pep10.out("       ORA 1,i");
        pep10.out("       STWA 4,s");
        pep10.out("_id10: SUBX 1,i");
        pep10.out("       BRNE _id7");
        pep10.out("       LDWA -2,s");
        pep10.out("       BRGE _id11");
        pep10.out("       ADDA 2,s");
        pep10.out("_id11: LDBX -3,s");
        pep10.out("       ANDX 1,i");
        pep10.out("       BREQ _id12");
        pep10.out("       NEGA");
        pep10.out("_id12: STWA 2,s");
        pep10.out("       LDBX -3,s");
        pep10.out("       ANDX 2,i");
        pep10.out("       BREQ _id13");
        pep10.out("       LDWA 4,s");
        pep10.out("       NEGA");
        pep10.out("       STWA 4,s");
        pep10.out("_id13: LDWX -6,s");
        pep10.out("       RET");

        return pep10.result;
    }

    private String newLabel() {
        return "__" + labelSeqNo++;
    }

    private void out(String s) {
        result.add(s);
    }

    private void out(String fmt, Object... objects) {
        result.add(String.format(fmt, objects));
    }

    private void translate(Label label) {
        out(label.toString());
    }

    private void translate(Nullary n) {
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

    private void translate(UnaryInteger ui) {
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

    private void translate(UnaryString us) {
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

    private void translate(BinaryString bs) {
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
