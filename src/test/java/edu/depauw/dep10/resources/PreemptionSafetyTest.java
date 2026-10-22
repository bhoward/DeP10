package edu.depauw.dep10.resources;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.simulator.Controller;
import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.simulator.StepCountController;
import edu.depauw.dep10.ui.MainFrame;
import edu.depauw.dep10.util.Word;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static edu.depauw.dep10.resources.PepHarness.mem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Checks that the routines in int32.pep are reentrant: two threads that call
 * them at the same time, with a simulated timer interrupt switching between
 * the threads every K instructions, still get the right answers.
 *
 * The routines used to keep their working variables in static words (.BLOCK),
 * so a switch in the middle of one routine let the other thread overwrite
 * those words. Now every working variable lives in a stack frame, and each
 * thread has its own stack.
 *
 * The "timer" is a Controller that, every K instructions, saves the whole
 * register context (A, X, H, SP, PC and NZVC) of the running thread and loads
 * the other thread's. That is what a real timer interrupt handler would do.
 * Running the same program for many different K moves the switch point
 * through every instruction of every routine, so a leftover shared variable
 * shows up as a wrong answer for at least one K.
 */
class PreemptionSafetyTest {
    private static final int T1_STACK = 0xC000;
    private static final int T2_STACK = 0xA000;
    private static final int T2_ENTRY = 3;      // second instruction slot, right after the first BR
    private static final int STEP_LIMIT = 3_000_000;

    private static final int T1 = 0xF000;       // thread 1 results (words)
    private static final int T2 = 0xF100;       // thread 2 results (words)
    private static final int DONE2 = 0xF0FE;

    private static final long A1 = -1_000_000L, B1 = 7L;
    private static final long A2 = 4_000_000_000L, B2 = 12_345L;
    private static final long READ_VALUE = -123_456_789L;
    private static final long[][] ROUNDS = {
            {123_456_789L, -1000L},
            {-2_000_000_011L, 97L},
            {1_999_999_999L, 3L},
    };

    /** A context switch every K instructions between two register contexts. */
    private static final class TimerController implements Controller {
        private final Controller parent;
        private final int k;
        private int count = 0;
        private final int[] saved = new int[9];      // the thread that is not running
        private int switches = 0;

        TimerController(Controller parent, int k, int pc, int sp) {
            this.parent = parent;
            this.k = k;
            saved[4] = pc;
            saved[3] = sp;
        }

        int switches() {
            return switches;
        }

        private void swap(State s) {
            int[] now = {
                    s.getA().value(), s.getX().value(), s.getH().value(), s.getSP().value(),
                    s.getPC().value(),
                    s.getN() ? 1 : 0, s.getZ() ? 1 : 0, s.getV() ? 1 : 0, s.getC() ? 1 : 0 };
            s.setA(Word.of(saved[0]));
            s.setX(Word.of(saved[1]));
            s.setH(Word.of(saved[2]));
            s.setSP(Word.of(saved[3]));
            s.setPC(Word.of(saved[4]));
            s.setN(saved[5] != 0);
            s.setZ(saved[6] != 0);
            s.setV(saved[7] != 0);
            s.setC(saved[8] != 0);
            System.arraycopy(now, 0, saved, 0, 9);
            switches++;
        }

        @Override
        public boolean perform(Operation op, State s, Word pc) {
            boolean r = parent.perform(op, s, pc);
            if (r && s.isRunning() && ++count % k == 0) {
                swap(s);
            }
            return r;
        }

        @Override public void end() { parent.end(); }
        @Override public void pause() { parent.pause(); }
        @Override public boolean isEnded() { return parent.isEnded(); }
        @Override public boolean isPaused() { return parent.isPaused(); }
        @Override public void resume(MainFrame f, List<Integer> breakpoints) { parent.resume(f, breakpoints); }
        @Override public void forward(MainFrame f) { parent.forward(f); }
        @Override public void backward(MainFrame f) { parent.backward(f); }
    }

    private static String push32(long v) {
        int lo = (int) (v & 0xFFFF), hi = (int) ((v >>> 16) & 0xFFFF);
        return String.format("        LDWA    0x%04X,i\n        PUSHA\n        LDWA    0x%04X,i\n        PUSHA\n", lo, hi);
    }

    /** After a two-result call: remainder is on top, quotient below it. Stores both and pops them. */
    private static String storeQuotRem(int addr) {
        return String.format(
                "        POPA\n        STWA    0x%04X,d\n        POPA\n        STWA    0x%04X,d\n"
                + "        POPA\n        STWA    0x%04X,d\n        POPA\n        STWA    0x%04X,d\n",
                addr + 4, addr + 6, addr, addr + 2);
    }

    /** After DMul: the product is in the first-pushed slot, the second operand's slot is on top. */
    private static String storeProduct(int addr) {
        return String.format(
                "        ADDSP   4,i\n        POPA\n        STWA    0x%04X,d\n        POPA\n        STWA    0x%04X,d\n",
                addr, addr + 2);
    }

    private static String program() {
        StringBuilder p = new StringBuilder();
        p.append("        BR      t1\n");
        p.append("        BR      t2\n");

        // thread 1: read a number, print it, then two divisions
        p.append("t1:\n");
        p.append("        SUBSP   4,i\n        CALL    DREAD,i\n");
        p.append(String.format("        POPA\n        STWA    0x%04X,d\n        POPA\n        STWA    0x%04X,d\n", T1, T1 + 2));
        p.append(String.format("        LDWA    0x%04X,d\n        PUSHA\n        LDWA    0x%04X,d\n        PUSHA\n", T1 + 2, T1));
        p.append("        CALL    DPRINTD,i\n        ADDSP   4,i\n");
        p.append(push32(A1)).append(push32(B1)).append("        CALL    DDiv,i\n").append(storeQuotRem(T1 + 4));
        p.append(push32(A2)).append(push32(B2)).append("        CALL    UDDiv,i\n").append(storeQuotRem(T1 + 12));
        p.append(String.format("w1:     LDWA    0x%04X,d\n        BREQ    w1\n", DONE2));
        p.append("        LDBA    0,i\n        STBA    pwrOff,d\n");

        // thread 2: several rounds of division and multiplication, no output
        p.append("t2:\n");
        for (int i = 0; i < ROUNDS.length; i++) {
            int base = T2 + 16 * i;
            p.append(push32(ROUNDS[i][0])).append(push32(ROUNDS[i][1]))
                    .append("        CALL    DDiv,i\n").append(storeQuotRem(base));
            p.append(push32(ROUNDS[i][0])).append(push32(ROUNDS[i][1]))
                    .append("        CALL    DMul,i\n").append(storeProduct(base + 8));
        }
        p.append(String.format("        LDWA    0x1,i\n        STWA    0x%04X,d\n", DONE2));
        p.append("w2:     BR      w2\n");

        p.append(".INCLUDELIB \"int32\"\n        .END\n");
        return p.toString();
    }

    private static long word32(State s, int addr) {
        return ((long) mem(s, addr) << 16 | mem(s, addr + 2)) & 0xFFFFFFFFL;
    }

    private static void check(State s, ByteArrayOutputStream out, int k) {
        String why = " (timer switch every " + k + " instructions)";
        assertEquals(Long.toString(READ_VALUE), out.toString(StandardCharsets.US_ASCII), "printed text" + why);
        assertEquals(READ_VALUE & 0xFFFFFFFFL, word32(s, T1), "DREAD result" + why);
        assertEquals((A1 / B1) & 0xFFFFFFFFL, word32(s, T1 + 4), "thread 1 DDiv quotient" + why);
        assertEquals((A1 % B1) & 0xFFFFFFFFL, word32(s, T1 + 8), "thread 1 DDiv remainder" + why);
        assertEquals(A2 / B2, word32(s, T1 + 12), "thread 1 UDDiv quotient" + why);
        assertEquals(A2 % B2, word32(s, T1 + 16), "thread 1 UDDiv remainder" + why);
        for (int i = 0; i < ROUNDS.length; i++) {
            int base = T2 + 16 * i;
            long a = ROUNDS[i][0], b = ROUNDS[i][1];
            assertEquals((a / b) & 0xFFFFFFFFL, word32(s, base), "thread 2 quotient " + i + why);
            assertEquals((a % b) & 0xFFFFFFFFL, word32(s, base + 4), "thread 2 remainder " + i + why);
            assertEquals((a * b) & 0xFFFFFFFFL, word32(s, base + 8), "thread 2 product " + i + why);
        }
    }

    private static TimerController runWithTimer(String obj, int k, ByteArrayOutputStream out, State[] result) {
        State s = PepHarness.load(obj, READ_VALUE + "\n");
        s.setOutput(out);
        Simulator sim = new Simulator(s);
        s.setSP(Word.of(T1_STACK));
        var timer = new TimerController(new PlainController(), k, T2_ENTRY, T2_STACK);
        sim.run(new StepCountController(timer, STEP_LIMIT));
        result[0] = s;
        return timer;
    }

    @Test
    @DisplayName("A coarse timer (switch every 50000 instructions): both threads finish with correct results")
    void coarseTimer() {
        String obj = PepHarness.assemble(program());
        var out = new ByteArrayOutputStream();
        State[] s = new State[1];
        var timer = runWithTimer(obj, 50_000, out, s);
        assertFalse(s[0].isRunning(), "machine should have powered off");
        assertEquals(true, timer.switches() > 0);
        check(s[0], out, 50_000);
    }

    @Test
    @DisplayName("Timer switch every K instructions, K = 1 to 400: all results correct")
    void preemptEveryKInstructions() {
        String obj = PepHarness.assemble(program());
        for (int k = 1; k <= 400; k++) {
            var out = new ByteArrayOutputStream();
            State[] s = new State[1];
            var timer = runWithTimer(obj, k, out, s);
            assertFalse(s[0].isRunning(), "machine should have powered off (K = " + k + ")");
            assertEquals(true, timer.switches() > 0);
            check(s[0], out, k);
        }
    }

    @Test
    @DisplayName("Timer switch every K instructions, a spread of larger and odd K")
    void preemptOddIntervals() {
        String obj = PepHarness.assemble(program());
        int[] ks = {401, 457, 509, 641, 757, 877, 1009, 1301, 1597, 2003, 3001, 4999, 7919};
        for (int k : ks) {
            var out = new ByteArrayOutputStream();
            State[] s = new State[1];
            runWithTimer(obj, k, out, s);
            assertFalse(s[0].isRunning(), "machine should have powered off (K = " + k + ")");
            check(s[0], out, k);
        }
    }

    @Test
    @DisplayName("int32.pep reserves no static storage: no .BLOCK, .WORD, .BYTE or .ASCII")
    void noStaticStorage() throws IOException {
        String text;
        try (InputStream in = PreemptionSafetyTest.class.getResourceAsStream("/int32.pep")) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String line : text.split("\n")) {
            String code = line.contains(";") ? line.substring(0, line.indexOf(';')) : line;
            String up = code.toUpperCase();
            for (String d : new String[] {".BLOCK", ".WORD", ".BYTE", ".ASCII", ".ASCIZ"}) {
                assertFalse(up.contains(d), "static storage directive " + d + " in: " + line.trim());
            }
        }
    }
}
