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
        return run(source, 200_000);
    }

    private static State run(String source, int stepLimit) {
        return run(source, stepLimit, null);
    }

    private static State run(String source, int stepLimit, String input) {
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
        return PepHarness.run(sb.toString(), stepLimit, input);
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
    @DisplayName("Narrowing, mixing with REAL, and operators LONGINT does not have are compile errors")
    void rejectsUnsupported() {
        assertTrue(rejects("VAR i : INTEGER; a : LONGINT; BEGIN i := a END."));
        assertTrue(rejects("VAR r : REAL; a : LONGINT; BEGIN a := a + r END."));
        assertTrue(rejects("VAR r : REAL; a : LONGINT; BEGIN IF a < r THEN a := 0 END END."));
        assertTrue(rejects("VAR a, b : LONGINT; BEGIN a := a / b END."));
        assertTrue(rejects("VAR a : LONGINT; BEGIN a := NOT a END."));
    }

    @Test
    @DisplayName("Multiplication: past 16 bits, negative, chained, mixed with INTEGER")
    void multiply() {
        State s = run("""
                VAR i, j : INTEGER; a, b, c, d : LONGINT;
                BEGIN
                  i := 30000; j := -3;
                  a := i;
                  b := a * a;
                  c := b * j;
                  d := a * j * j + i + i
                END.
                """);
        // slots: i=0 j=1 a=2 b=4 c=6 d=8
        assertEquals(900000000, lng(s, 4));
        assertEquals(900000000 * -3, lng(s, 6));
        assertEquals(30000 * 9 + 60000, lng(s, 8));
    }

    @Test
    @DisplayName("DIV and MOD truncate toward zero, like INTEGER")
    void divMod() {
        int[][] cases = { { 120000, 7 }, { -120000, 7 }, { 120000, -7 }, { -120000, -7 }, { 5, 100000 }, { 100000, 100000 } };
        for (int[] c : cases) {
            // build values in LONGINT with INTEGER pieces: v = hi * 1000 + lo
            State s = run(String.format("""
                    VAR m, p1, q1, p2, q2 : INTEGER; a, b, qd, rm : LONGINT;
                    BEGIN
                      m := 1000;
                      p1 := %d; q1 := %d; p2 := %d; q2 := %d;
                      a := p1; a := a * m + q1;
                      b := p2; b := b * m + q2;
                      qd := a DIV b;
                      rm := a MOD b
                    END.
                    """, c[0] / 1000, c[0] % 1000, c[1] / 1000, c[1] % 1000));
            // slots: m=0 p1=1 q1=2 p2=3 q2=4 a=5 b=7 qd=9 rm=11
            assertEquals(c[0], lng(s, 5), "a for " + c[0] + "," + c[1]);
            assertEquals(c[1], lng(s, 7), "b for " + c[0] + "," + c[1]);
            assertEquals(c[0] / c[1], lng(s, 9), c[0] + " DIV " + c[1]);
            assertEquals(c[0] % c[1], lng(s, 11), c[0] + " MOD " + c[1]);
        }
    }

    @Test
    @DisplayName("Unary minus on LONGINT, including 0 and values with a zero low word")
    void negate() {
        int[] vals = { 0, 1, -1, 65536, -65536, 70000, 32767000, -32767000, 123 };
        for (int v : vals) {
            State s = run(String.format("""
                    VAR m, p, q : INTEGER; a, b, c : LONGINT;
                    BEGIN
                      m := 1000; p := %d; q := %d;
                      a := p; a := a * m + q;
                      b := -a;
                      c := -b + b + b
                    END.
                    """, v / 1000, v % 1000));
            // slots: m=0 p=1 q=2 a=3 b=5 c=7
            assertEquals(v, lng(s, 3));
            assertEquals(-v, lng(s, 5), "-" + v);
            assertEquals(-v, lng(s, 7), "c " + v);
        }
    }

    @Test
    @DisplayName("Comparisons agree with Java for many pairs, in IF conditions and as BOOLEAN values")
    void comparisons() {
        int[] vals = { 0, 1, -1, 7, 32767, 32768, 65535, 65536, 70000, -70000, -32768, -32769, 32767000, -32767000,
                1000, 999, 40000, 30000, 100000, -100000 };
        for (int x : vals) {
            for (int y : new int[] { 0, 1, -1, 32768, 65536, 70000, -70000, 40000, 30000, x, x + 1, x - 1 }) {
                State s = run(String.format("""
                        VAR m, p1, q1, p2, q2 : INTEGER; a, b : LONGINT; t : BOOLEAN;
                            r1, r2, r3, r4, r5, r6, r7 : INTEGER;
                        BEGIN
                          m := 1000;
                          p1 := %d; q1 := %d; p2 := %d; q2 := %d;
                          a := p1; a := a * m + q1;
                          b := p2; b := b * m + q2;
                          IF a = b THEN r1 := 1 ELSE r1 := 0 END;
                          IF a # b THEN r2 := 1 ELSE r2 := 0 END;
                          IF a < b THEN r3 := 1 ELSE r3 := 0 END;
                          IF a <= b THEN r4 := 1 ELSE r4 := 0 END;
                          IF a > b THEN r5 := 1 ELSE r5 := 0 END;
                          IF a >= b THEN r6 := 1 ELSE r6 := 0 END;
                          t := a < b;
                          IF t THEN r7 := 1 ELSE r7 := 0 END
                        END.
                        """, x / 1000, x % 1000, y / 1000, y % 1000));
                // slots: m=0 p1=1 q1=2 p2=3 q2=4 a=5 b=7 t=9 r1=10 .. r7=16
                String what = x + " vs " + y;
                assertEquals(x == y ? 1 : 0, word(s, 10), what + " =");
                assertEquals(x != y ? 1 : 0, word(s, 11), what + " #");
                assertEquals(x < y ? 1 : 0, word(s, 12), what + " <");
                assertEquals(x <= y ? 1 : 0, word(s, 13), what + " <=");
                assertEquals(x > y ? 1 : 0, word(s, 14), what + " >");
                assertEquals(x >= y ? 1 : 0, word(s, 15), what + " >=");
                assertEquals(x < y ? 1 : 0, word(s, 16), what + " as value");
            }
        }
    }

    @Test
    @DisplayName("LONGINT in a WHILE loop: count up past 16 bits")
    void loop() {
        State s = run("""
                VAR i : INTEGER; n, limit, step : LONGINT;
                BEGIN
                  i := 20000; step := i;
                  limit := step * 5;
                  n := 0 + i - i;
                  i := 0;
                  WHILE n < limit DO
                    n := n + step;
                    i := i + 1
                  END
                END.
                """);
        // slots: i=0 n=1 limit=3 step=5
        assertEquals(5, word(s, 0));
        assertEquals(100000, lng(s, 1));
    }

    @Test
    @DisplayName("demo/longint-demo.dcl prints 1! to 12! and Fibonacci(45) with WriteLong")
    void demoProgram() throws Exception {
        String src = java.nio.file.Files.readString(java.nio.file.Path.of("demo/longint-demo.dcl"));
        StringBuilder expected = new StringBuilder();
        long f = 1;
        for (int k = 1; k <= 12; k++) {
            f *= k;
            expected.append(" ").append(k).append(" ").append(f).append("\n");
        }
        long a = 0, b = 1;
        for (int k = 0; k < 45; k++) {
            long t = a + b;
            a = b;
            b = t;
        }
        expected.append(" ").append(a).append("\n");
        assertEquals(expected.toString(), printed(src));
    }

    private static Object literal(String src) {
        Reporter r = new Reporter(new PrintStream(new ByteArrayOutputStream()));
        Object v = new Scanner(src, r).scanTokens().get(0).literal;
        assertFalse(r.hadError(), src);
        return v;
    }

    @Test
    @DisplayName("Literal type is the minimal type containing the number")
    void literalTypes() {
        assertEquals(Integer.valueOf(0), literal("0"));
        assertEquals(Integer.valueOf(32767), literal("32767"));
        assertEquals(Long.valueOf(32768L), literal("32768"));
        assertEquals(Long.valueOf(2147483647L), literal("2147483647"));
        assertEquals(Integer.valueOf(0x7FFF), literal("7FFFH"));
        assertEquals(Long.valueOf(0xFFFFL), literal("0FFFFH"));
        assertEquals(Long.valueOf(0x7FFFFFFFL), literal("7FFFFFFFH"));
    }

    @Test
    @DisplayName("Literal beyond 32 bits is a scanner error")
    void literalTooLarge() {
        Reporter r = new Reporter(new PrintStream(new ByteArrayOutputStream()));
        new Scanner("2147483648", r).scanTokens();
        assertTrue(r.hadError());
    }

    @Test
    @DisplayName("LONGINT literals in assignments, constants and expressions")
    void longLiterals() {
        State s = run("""
                CONST big = 100000; huge = big * 20000; small = big - 99990;
                VAR a, b, c, d : LONGINT; i : INTEGER;
                BEGIN
                  a := 100000;
                  b := a + 2147000000;
                  c := -70000;
                  d := huge;
                  i := small;
                  IF a < 123456 THEN a := a + 1 END
                END.
                """);
        // slots: big=0 huge=2 small=4 (constants take slots too), a=5 b=7 c=9 d=11 i=13
        assertEquals(100000, lng(s, 0));
        assertEquals(2000000000, lng(s, 2));
        assertEquals(10, word(s, 4));
        assertEquals(100001, lng(s, 5));
        assertEquals(100000 + 2147000000, lng(s, 7));
        assertEquals(-70000, lng(s, 9));
        assertEquals(2000000000, lng(s, 11));
        assertEquals(10, word(s, 13));
    }

    private static String printed(String source) {
        return printed(source, null);
    }

    private static String printed(String source, String input) {
        PrintStream saved = System.out;
        var buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true));
        try {
            run(source, 5_000_000, input);
        } finally {
            System.setOut(saved);
        }
        return buf.toString().replace("\r", "");
    }

    @Test
    @DisplayName("WriteLong prints signed 32-bit values, like WriteInt with a leading space")
    void writeLong() {
        String out = printed("""
                VAR a : LONGINT; i : INTEGER;
                BEGIN
                  a := 0; WriteLong(a);
                  a := 7; WriteLong(a);
                  a := 100000; WriteLong(a);
                  a := -123456; WriteLong(a);
                  a := 2147483647; WriteLong(a);
                  a := -2147483647 - 1; WriteLong(a);
                  i := -5; WriteLong(i);
                  WriteLong(1000000);
                  WriteLong(a + a + 1);
                  WriteLn()
                END.
                """);
        assertEquals(" 0 7 100000 -123456 2147483647 -2147483648 -5 1000000 1\n", out);
    }

    @Test
    @DisplayName("WriteLong together with LONGINT arithmetic does not duplicate libraries")
    void writeLongWithArithmetic() {
        String out = printed("""
                VAR f : LONGINT; i : INTEGER;
                BEGIN
                  f := 1;
                  FOR i := 1 TO 12 DO f := f * i END;
                  WriteLong(f);
                  WriteLong(f DIV 1000);
                  WriteLong(f MOD 1000);
                  WriteLn()
                END.
                """);
        assertEquals(" 479001600 479001 600\n", out);
    }

    @Test
    @DisplayName("ReadLong reads signed 32-bit decimal values, skipping blanks and line ends")
    void readLong() {
        String out = printed("""
                VAR a : LONGINT; i : INTEGER;
                BEGIN
                  FOR i := 1 TO 9 DO
                    ReadLong(a); WriteLong(a)
                  END;
                  WriteLn()
                END.
                """, "0 7\n  100000\n-123456 +42 2147483647\n-2147483648\n99999999999 5 ");
        assertEquals(" 0 7 100000 -123456 42 2147483647 -2147483648 1215752191 5\n", out);
    }

    @Test
    @DisplayName("ReadLong works on a local LONGINT and through a VAR parameter")
    void readLongVar() {
        String out = printed("""
                PROCEDURE Get(VAR x : LONGINT);
                BEGIN
                  ReadLong(x)
                END Get;
                PROCEDURE Twice();
                VAR t : LONGINT;
                BEGIN
                  Get(t); WriteLong(t + t)
                END Twice;
                BEGIN
                  Twice(); Twice(); WriteLn()
                END.
                """, "70000 -3000000 ");
        assertEquals(" 140000 -6000000\n", out);
    }

    @Test
    @DisplayName("ReadLong reads a number and echoes it back with ReadInt-style loops")
    void readLongSum() {
        String out = printed("""
                VAR n, sum, x : LONGINT; i : INTEGER;
                BEGIN
                  ReadLong(n);
                  sum := 0;
                  FOR i := 1 TO 3 DO
                    ReadLong(x); sum := sum + x
                  END;
                  WriteLong(n); WriteLong(sum); WriteLn()
                END.
                """, "3 1000000 2000000 3000000\n");
        assertEquals(" 3 6000000\n", out);
    }

    @Test
    @DisplayName("A LONGINT FOR counter runs past the INTEGER range")
    void forLongCounter() {
        String out = printed("""
                VAR k, sum : LONGINT;
                BEGIN
                  sum := 0;
                  FOR k := 32765 TO 32770 DO
                    sum := sum + k; WriteLong(k)
                  END;
                  WriteLong(sum); WriteLn()
                END.
                """);
        assertEquals(" 32765 32766 32767 32768 32769 32770 196605\n", out);
    }

    @Test
    @DisplayName("A LONGINT FOR counter with a negative step, a LONGINT step and LONGINT bounds")
    void forLongSteps() {
        String out = printed("""
                CONST big = 100000;
                VAR k : LONGINT; n : INTEGER;
                BEGIN
                  FOR k := big TO 99998 BY -1 DO WriteLong(k) END;
                  WriteLn();
                  FOR k := -200000 TO 200000 BY big DO WriteLong(k) END;
                  WriteLn();
                  FOR k := 5 TO 1 DO WriteLong(k) END;
                  n := 3;
                  FOR k := 1 TO n BY 1 DO WriteLong(k) END;
                  WriteLn()
                END.
                """);
        assertEquals(" 100000 99999 99998\n -200000 -100000 0 100000 200000\n 1 2 3\n", out);
    }

    @Test
    @DisplayName("A LONGINT FOR counter works as a local variable and inside nested loops")
    void forLongLocal() {
        String out = printed("""
                PROCEDURE Count(n : LONGINT);
                VAR k : LONGINT; i : INTEGER;
                BEGIN
                  FOR k := n TO n + 2 DO
                    FOR i := 1 TO 2 DO WriteLong(k * 10 + i) END
                  END
                END Count;
                BEGIN
                  Count(70000); WriteLn()
                END.
                """);
        assertEquals(" 700001 700002 700011 700012 700021 700022\n", out);
    }

    @Test
    @DisplayName("FOR rejects a LONGINT step for an INTEGER counter and REAL counters")
    void forErrors() {
        for (String body : new String[] {
                "VAR i : INTEGER; BEGIN FOR i := 1 TO 3 BY 100000 DO END END.",
                "VAR i : INTEGER; BEGIN FOR i := 1 TO 100000 DO END END.",
                "VAR x : REAL; BEGIN FOR x := 1 TO 3 DO END END." }) {
            var err = new ByteArrayOutputStream();
            var reporter = new Reporter(new PrintStream(err));
            DeCLan.run(body, reporter);
            assertTrue(reporter.hadError(), body);
        }
    }
}
