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
    private boolean usesDoubleword;
    private boolean usesMultiply;
    private boolean usesDivide;
    private boolean usesPrintLong;

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

        // ddprint includes daddsub and ddiv32 itself, so skip them here when it is used
        if (usesDoubleword && !usesPrintLong) {
            // DADD and DSUB are callable routines, so they go after the code that ends in RET
            out(".INCLUDELIB \"daddsub\"");
        }
        if (usesMultiply) {
            out(".INCLUDELIB \"dmul\"");
        }
        if (usesDivide && !usesPrintLong) {
            out(".INCLUDELIB \"ddiv32\"");
        }
        if (usesPrintLong) {
            out(".INCLUDELIB \"ddprint\"");
        }

        return result;
    }

    void writeWriteLong() {
        if (!usesPrintLong) {
            return;
        }
        // WriteLong(n): a space, then the signed decimal value. The argument is
        // already in the layout DPRINTD wants (high word at the lower address),
        // but DPRINTD takes its own copy pushed low word first.
        out("WriteLong: LDBA ' ',i");
        out("       STBA charOut,d");
        out("       LDWA 4,s");
        out("       PUSHA");
        out("       LDWA 4,s");
        out("       PUSHA");
        out("       CALL DPRINTD,i");
        out("       ADDSP 4,i");
        out("       RET");
    }

    void writeRuntime() {
        out("ReadInt: @DECI 2,sf");
        out("       RET");
        out("WriteInt: LDBA ' ',i");
        out("       STBA charOut,d");
        out("       @DECO 2,s");
        out("       RET");
        writeWriteLong();
        out("WriteLn: LDBA '\\n',i");
        out("       STBA charOut,d");
        out("       RET");
        // _imul does 16-bit signed integer multiplication,
        // giving a 32-bit result.
        // It preserves the X register.
        // Stack before call:
        //   SP+0: B (multiplier)
        //   SP+2: A (multiplicand)
        // Stack during call:
        //   SP-4: save X (frame pointer)
        //   SP-2: temporary
        //   SP+0: return address
        //   SP+2: B (modified)
        //   SP+4: A
        // Stack after call:
        //   SP+0: A*B (product) high word; discard if only a 16-bit result
        //   SP+2: A*B (product) low word
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
        // _idiv does 16-bit signed integer division,
        // giving a 16-bit quotient and remainder.
        // The quotient is rounded toward zero (truncating division),
        // and the remainder has the same sign as the dividend -- these
        // are the same as the Java / and % operators.
        // Satisfies (A/B)*B + (A%B) = A, and |A%B| < |divisor| (if non-zero).
        // If the divisor is zero, the quotient is arbitrary and the
        // remainder is the dividend.
        // Special case: -32768 / -1 = -32768.
        // It preserves the X register.
        // Stack before call:
        //   SP+0: B (divisor)
        //   SP+2: A (dividend)
        // Stack during call:
        //   SP-6: save X (frame pointer)
        //   SP-4: temp
        //   SP-2: temp
        //   SP+0: return address
        //   SP+2: B
        //   SP+4: A (modified)
        // Stack after call:
        //   SP+0: A%B (remainder)
        //   SP+2: A/B (quotient)
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

    // Compare the two 32-bit values on top of the stack (A below B, each with
    // its high word at the lower address), branching to yes if A = B and to no
    // otherwise. The stack is left as it was; the caller removes the 8 bytes.
    void emitLongEqual(String yes, String no) {
        out("LDWA 4,s");
        out("CPWA 0,s");
        out("BRNE %s,i", no);
        out("LDWA 6,s");
        out("CPWA 2,s");
        out("BRNE %s,i", no);
        out("BR %s,i", yes);
    }

    // Same, branching to yes if A < B as signed 32-bit values. The high words
    // decide unless they are equal, in which case the low words are compared
    // as unsigned numbers by flipping their top bit and comparing as signed.
    void emitLongLess(String yes, String no) {
        out("LDWA 4,s");
        out("CPWA 0,s");
        out("BRLT %s,i", yes);
        out("BRGT %s,i", no);
        String y2 = newLabel();
        String n2 = newLabel();
        out("LDWA 2,s");
        out("XORA 0x8000,i");
        out("SUBSP 2,i");
        out("STWA 0,s");
        out("LDWA 8,s");
        out("XORA 0x8000,i");
        out("CPWA 0,s");
        out("BRLT %s,i", y2);
        out("BR %s,i", n2);
        out("%s: ADDSP 2,i", y2);
        out("BR %s,i", yes);
        out("%s: ADDSP 2,i", n2);
        out("BR %s,i", no);
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
        case LADD:
            // Two 32-bit values, each high word at the lower address. DADD replaces
            // the first operand with the sum; discard the second.
            usesDoubleword = true;
            out("CALL DADD,i");
            out("ADDSP 4,i");
            break;
        case LSUB:
            usesDoubleword = true;
            out("CALL DSUB,i");
            out("ADDSP 4,i");
            break;
        case LMUL:
            usesMultiply = true;
            out("CALL DMul,i");
            out("ADDSP 4,i");
            break;
        case LDIV:
            // DDiv leaves the quotient in the first operand's slot and the
            // remainder in the second's; keep the quotient
            usesDivide = true;
            out("CALL DDiv,i");
            out("ADDSP 4,i");
            break;
        case LMOD:
            // Keep the remainder: move it over the quotient, then drop the top
            usesDivide = true;
            out("CALL DDiv,i");
            out("LDWA 0,s");
            out("STWA 4,s");
            out("LDWA 2,s");
            out("STWA 6,s");
            out("ADDSP 4,i");
            break;
        case LNEG: {
            // -x = ~x + 1: negate the low word, invert the high word, and add
            // the carry into it when the low word was zero
            String done = newLabel();
            out("LDWA 0,s");
            out("NOTA");
            out("STWA 0,s");
            out("LDWA 2,s");
            out("NEGA");
            out("STWA 2,s");
            out("BRNE %s,i", done);
            out("LDWA 0,s");
            out("ADDA 1,i");
            out("STWA 0,s");
            out("%s: NOP", done);
            break;
        }
        case LEQL: {
            String yes = newLabel();
            String no = newLabel();
            String end = newLabel();
            emitLongEqual(yes, no);
            out("%s: LDWA 1,i", yes);
            out("BR %s,i", end);
            out("%s: LDWA 0,i", no);
            out("%s: ADDSP 6,i", end);
            out("STWA 0,s");
            break;
        }
        case LLT: {
            String yes = newLabel();
            String no = newLabel();
            String end = newLabel();
            emitLongLess(yes, no);
            out("%s: LDWA 1,i", yes);
            out("BR %s,i", end);
            out("%s: LDWA 0,i", no);
            out("%s: ADDSP 6,i", end);
            out("STWA 0,s");
            break;
        }
        case LONG: {
            // Widen the INTEGER on top of the stack to a 32-bit value: the old word
            // becomes the low word, and the new high word is its sign extension.
            String neg = newLabel();
            out("LDWA 0,s");
            out("SUBSP 2,i");
            out("STWA 2,s");
            out("LDWA 0,i");
            out("STWA 0,s");
            out("LDWA 2,s");
            out("BRGE %s,i", neg);
            out("LDWA -1,i");
            out("STWA 0,s");
            out("%s: NOP", neg);
            break;
        }
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
        case LLD_CONST:
            out("LDWA %d,i", (short) (ui.value >>> 16));
            out("SUBSP 4,i");
            out("STWA 0,s");
            out("LDWA %d,i", (short) ui.value);
            out("STWA 2,s");
            break;
        case LLD_GLOBAL:
            out("SUBSP 4,i");
            out("LDWA _g%d,d", ui.value);
            out("STWA 0,s");
            out("LDWA _g%d,d", ui.value + 1);
            out("STWA 2,s");
            break;
        case LLD_LOCAL:
            // low word is slot n, high word is slot n+1 (the lower address)
            out("SUBSP 4,i");
            out("LDWA %d,x", -2 * (ui.value + 1));
            out("STWA 0,s");
            out("LDWA %d,x", -2 * ui.value);
            out("STWA 2,s");
            break;
        case LLD_VARP:
            // slot n holds the address of the high word
            out("LDWA %d,x", -2 * ui.value);
            out("SUBSP 4,i");
            out("STWA 0,s");
            out("LDWA 0,sf");
            out("STWA -2,s");
            out("LDWA 0,s");
            out("ADDA 2,i");
            out("STWA 0,s");
            out("LDWA 0,sf");
            out("STWA 2,s");
            out("LDWA -2,s");
            out("STWA 0,s");
            break;
        case LST_GLOBAL:
            out("LDWA 0,s");
            out("STWA _g%d,d", ui.value);
            out("LDWA 2,s");
            out("STWA _g%d,d", ui.value + 1);
            out("ADDSP 4,i");
            break;
        case LST_LOCAL:
            out("LDWA 0,s");
            out("STWA %d,x", -2 * (ui.value + 1));
            out("LDWA 2,s");
            out("STWA %d,x", -2 * ui.value);
            out("ADDSP 4,i");
            break;
        case LST_VARP:
            out("LDWA %d,x", -2 * ui.value);
            out("STWA -2,s");
            out("LDWA 0,s");
            out("STWA -2,sf");
            out("LDWA -2,s");
            out("ADDA 2,i");
            out("STWA -2,s");
            out("LDWA 2,s");
            out("STWA -2,sf");
            out("ADDSP 4,i");
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
            if (us.value.equals("WriteLong")) {
                usesPrintLong = true;
            }
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
        case BRLEQL: {
            String yes = newLabel();
            String no = newLabel();
            emitLongEqual(yes, no);
            out("%s: ADDSP 8,i", yes);
            out("BR %s,i", bs.left);
            out("%s: ADDSP 8,i", no);
            out("BR %s,i", bs.right);
            break;
        }
        case BRLLT: {
            String yes = newLabel();
            String no = newLabel();
            emitLongLess(yes, no);
            out("%s: ADDSP 8,i", yes);
            out("BR %s,i", bs.left);
            out("%s: ADDSP 8,i", no);
            out("BR %s,i", bs.right);
            break;
        }
        default:
            System.err.println("Unsupported instruction: " + bs);
        }
    }
}
