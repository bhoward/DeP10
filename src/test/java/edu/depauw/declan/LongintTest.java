package edu.depauw.declan;

import edu.depauw.dep10.resources.PepHarness;
import edu.depauw.dep10.simulator.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static edu.depauw.dep10.resources.PepHarness.mem;

/**
 * End-to-end tests for the LONGINT type in the Declan compiler: declarations,
 * assignment, widening of INTEGER, + and -, in globals, locals, value
 * parameters, and VAR parameters. Each test compiles a Declan program, runs the
 * generated Pep/10 code in the simulator, and reads the global variables back.
 *
 * Global slots are dumped to 0xF000 + 2 * slot. A LONGINT takes two slots, high
 * word first.
 */
class LongintTest {

    private static final int DUMP = 0xF000;

    private static String compile(String source) {
        var err = new ByteArrayOutputStream();
        var reporter = new Reporter(new PrintStream(err));
        String asm = DeCLan.run(source, reporter);
        assertFalse(reporter.hadError(), "compile errors: " + err);
        return asm;
    }

    private static State run(String source) {
        String asm = compile(source);

        int slots = 0;
        Matcher m = Pattern.compile("^_g(\\d+):", Pattern.MULTILINE).matcher(asm);
        while (m.find()) {
            slots = Math.max(slots, Integer.parseInt(m.group(1)) + 1);
        }

        StringBuilder sb = new StringBuilder();
        // The generated runtime calls the OS I/O macros, which the bare-metal
        // environment does not define; these programs do no I/O, so stub them out.
        sb.append(".DEFMACRO DECI, 2\n.ENDMACRO\n.DEFMACRO DECO, 2\n.ENDMACRO\n");
        // The program's final RET returns to done
        sb.append("        LDWA    done,i\n        PUSHA\n");
        sb.append(asm);
        sb.append("\ndone:   NOP\n");
        for (int k = 0; k < slots; k++) {
            sb.append(String.format("        LDWA    _g%d,d\n        STWA    0x%04X,d\n", k, DUMP + 2 * k));
        }
        sb.append("        LDBA    0,i\n        STBA    pwrOff,d\n        .END\n");
        return PepHarness.run(sb.toString());
    }

    private static int word(State s, int slot) {
        return (short) mem(s, DUMP + 2 * slot);
    }

    private static int lng(State s, int slot) {
        return (mem(s, DUMP + 2 * slot) << 16) | mem(s, DUMP + 2 * (slot + 1));
    }

    @Test
    @DisplayName("Global LONGINT: widen INTEGER, add, subtract (values past 16 bits)")
    void globalAddSub() {
        State s = run("""
                VAR i, j : INTEGER; a, b, c, d, e : LONGINT;
                BEGIN
                  i := 30000; j := 25000;
                  a := i; b := j;
                  c := a + b;
                  d := c + c - b;
                  e := b - d
                END.
                """);
        // slots: i=0 j=1 a=2 b=4 c=6 d=8 e=10
        assertEquals(30000, lng(s, 2));
        assertEquals(25000, lng(s, 4));
        assertEquals(55000, lng(s, 6));
        assertEquals(55000 + 55000 - 25000, lng(s, 8));
        assertEquals(25000 - 85000, lng(s, 10));
        assertEquals(30000, word(s, 0));
        assertEquals(25000, word(s, 1));
    }

    @Test
    @DisplayName("Widening a negative INTEGER sign-extends")
    void widenNegative() {
        State s = run("""
                VAR i : INTEGER; a, b : LONGINT;
                BEGIN
                  i := -5; a := i;
                  b := a + a + a
                END.
                """);
        assertEquals(-5, lng(s, 1));
        assertEquals(-15, lng(s, 3));
    }

    @Test
    @DisplayName("INTEGER operands are widened automatically in mixed expressions")
    void mixedExpression() {
        State s = run("""
                VAR i : INTEGER; a, b : LONGINT;
                BEGIN
                  i := 32767; a := i;
                  b := a + a + i + i + 1000
                END.
                """);
        assertEquals(4 * 32767 + 1000, lng(s, 3));
    }

    @Test
    @DisplayName("Carries and borrows between the two words, and wraparound")
    void carries() {
        State s = run("""
                VAR i, j : INTEGER; a, b, c, d : LONGINT;
                BEGIN
                  i := 32767; j := 1;
                  a := i + i;
                  a := a + j;
                  b := a + j;
                  c := b - j - j;
                  d := c - c
                END.
                """);
        // 32767 + 32767 is done in INTEGER, so it wraps: use the long results instead
        int ii = (short) (32767 + 32767);
        assertEquals(ii + 1, lng(s, 2));
        assertEquals(ii + 2, lng(s, 4));
        assertEquals(ii, lng(s, 6));
        assertEquals(0, lng(s, 8));
    }

    @Test
    @DisplayName("Local LONGINT and value LONGINT parameter, with INTEGER argument widened")
    void localsAndParams() {
        State s = run("""
                VAR r : LONGINT; n : INTEGER;
                PROCEDURE addTwice(x : LONGINT; VAR out : LONGINT);
                  VAR t : LONGINT;
                  BEGIN
                    t := x + x;
                    out := t + x
                  END addTwice;
                BEGIN
                  n := 30000;
                  addTwice(n, r)
                END.
                """);
        assertEquals(90000, lng(s, 0));
        assertEquals(30000, word(s, 2));
    }

    @Test
    @DisplayName("VAR LONGINT parameters, including passing one on, and a reference to a local")
    void varParams() {
        State s = run("""
                VAR r, q : LONGINT;
                PROCEDURE inc(VAR v : LONGINT);
                  BEGIN v := v + v END inc;
                PROCEDURE outer(VAR w : LONGINT);
                  BEGIN inc(w) END outer;
                PROCEDURE twice(n : INTEGER; VAR out : LONGINT);
                  VAR t : LONGINT;
                  BEGIN t := n; inc(t); inc(t); out := t END twice;
                BEGIN
                  twice(20000, r);
                  q := r;
                  outer(q)
                END.
                """);
        assertEquals(80000, lng(s, 0));
        assertEquals(160000, lng(s, 2));
    }

    @Test
    @DisplayName("Mixed INTEGER and LONGINT locals and parameters do not overlap in the frame")
    void frameLayout() {
        State s = run("""
                VAR g1, g2, k : INTEGER; r, big : LONGINT;
                PROCEDURE mix(a : INTEGER; b : LONGINT; c : INTEGER; VAR out : LONGINT);
                  VAR p : INTEGER; t : LONGINT; q : INTEGER;
                  BEGIN
                    p := 111; q := 222;
                    t := b + a;
                    t := t + c;
                    g1 := p; g2 := q;
                    out := t
                  END mix;
                BEGIN
                  k := 30000;
                  big := k;
                  big := big + big + big + big;
                  mix(7, big, 5, r)
                END.
                """);
        // slots: g1=0 g2=1 k=2 r=3 big=5
        assertEquals(111, word(s, 0));
        assertEquals(222, word(s, 1));
        assertEquals(120012, lng(s, 3));
        assertEquals(120000, lng(s, 5));
    }

    @Test
    @DisplayName("Programs with no LONGINT do not pull in the 32-bit library")
    void noLibraryWithoutLong() {
        String asm = compile("""
                VAR x : INTEGER;
                BEGIN x := 1 + 2 END.
                """);
        assertFalse(asm.contains("INCLUDELIB"));
    }

    @Test
    @DisplayName("Programs with LONGINT include daddsub after the code")
    void libraryWithLong() {
        String asm = compile("""
                VAR a : LONGINT;
                BEGIN a := a + a END.
                """);
        assertTrue(asm.contains(".INCLUDELIB \"daddsub\""));
    }

    private static boolean rejects(String source) {
        var err = new ByteArrayOutputStream();
        var reporter = new Reporter(new PrintStream(err));
        DeCLan.run(source, reporter);
        return reporter.hadError();
    }

    @Test
    @DisplayName("Narrowing, mixing with REAL, and unsupported operators are compile errors")
    void rejectsUnsupported() {
        assertTrue(rejects("VAR i : INTEGER; a : LONGINT; BEGIN i := a END."));
        assertTrue(rejects("VAR r : REAL; a : LONGINT; BEGIN a := a + r END."));
        assertTrue(rejects("VAR a, b : LONGINT; BEGIN a := a * b END."));
        assertTrue(rejects("VAR a, b : LONGINT; BEGIN a := -b END."));
        assertTrue(rejects("VAR a, b : LONGINT; BEGIN IF a < b THEN a := b END END."));
    }
}
