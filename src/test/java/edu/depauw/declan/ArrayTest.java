package edu.depauw.declan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Compiles Declan programs with TYPE declarations and arrays, runs them on the
 * simulator and checks what they print. WriteLong stands in for WriteInt, which
 * needs the OS.
 */
class ArrayTest {
    private static String printed(String source) {
        return LongintTest.printed(source);
    }

    private static boolean hasError(String source) {
        var err = new ByteArrayOutputStream();
        var reporter = new Reporter(new PrintStream(err));
        DeCLan.run(source, reporter);
        return reporter.hadError();
    }

    @Test
    @DisplayName("A global array can be filled and summed")
    void globalArray() {
        String out = printed("""
                VAR x : INTEGER; a : ARRAY 5 OF INTEGER; y, i, sum : INTEGER;
                BEGIN
                  x := 11; y := 22;
                  FOR i := 0 TO 4 DO a[i] := i * i END;
                  sum := 0;
                  FOR i := 0 TO 4 DO sum := sum + a[i] END;
                  WriteLong(sum); WriteLong(x); WriteLong(y); WriteLong(a[4]); WriteLn()
                END.
                """);
        assertEquals(" 30 11 22 16\n", out);
    }

    @Test
    @DisplayName("A local array does not disturb the other locals and parameters")
    void localArray() {
        String out = printed("""
                PROCEDURE P(n : INTEGER);
                VAR x : INTEGER; a : ARRAY 4 OF INTEGER; y, i : INTEGER;
                BEGIN
                  x := 7; y := 9;
                  FOR i := 0 TO 3 DO a[i] := n + i END;
                  WriteLong(x); WriteLong(y); WriteLong(n);
                  WriteLong(a[0]); WriteLong(a[1]); WriteLong(a[2]); WriteLong(a[3]);
                  WriteLn()
                END P;
                BEGIN
                  P(100); P(5)
                END.
                """);
        assertEquals(" 7 9 100 100 101 102 103\n 7 9 5 5 6 7 8\n", out);
    }

    @Test
    @DisplayName("TYPE declarations name arrays, constants give the length, arrays nest")
    void typeDeclarations() {
        String out = printed("""
                CONST n = 3;
                TYPE Vec = ARRAY n OF INTEGER;
                     Mat = ARRAY 2 OF Vec;
                VAR m : Mat; i, j : INTEGER;
                BEGIN
                  FOR i := 0 TO 1 DO
                    FOR j := 0 TO n - 1 DO m[i][j] := 10 * i + j END
                  END;
                  FOR i := 0 TO 1 DO
                    FOR j := 0 TO n - 1 DO WriteLong(m[i][j]) END
                  END;
                  WriteLn()
                END.
                """);
        assertEquals(" 0 1 2 10 11 12\n", out);
    }

    @Test
    @DisplayName("Arrays of LONGINT keep both words of every element")
    void longintArray() {
        String out = printed("""
                TYPE Big = ARRAY 4 OF LONGINT;
                VAR a : Big; i : INTEGER;
                PROCEDURE Show(k : INTEGER);
                VAR t : ARRAY 3 OF LONGINT; j : INTEGER;
                BEGIN
                  FOR j := 0 TO 2 DO t[j] := 70000 * (j + k) END;
                  FOR j := 0 TO 2 DO WriteLong(t[j]) END;
                  WriteLn()
                END Show;
                BEGIN
                  FOR i := 0 TO 3 DO a[i] := 100000 * (i - 1) END;
                  FOR i := 0 TO 3 DO WriteLong(a[i]) END;
                  WriteLn();
                  Show(1)
                END.
                """);
        assertEquals(" -100000 0 100000 200000\n 70000 140000 210000\n", out);
    }

    @Test
    @DisplayName("An index may itself be an array element, and BOOLEAN elements work as conditions")
    void computedIndex() {
        String out = printed("""
                VAR a : ARRAY 4 OF INTEGER; b : ARRAY 3 OF BOOLEAN;
                BEGIN
                  a[0] := 3; a[1] := 2; a[2] := 1; a[3] := 0;
                  b[1] := TRUE;
                  WriteLong(a[a[0]]); WriteLong(a[a[a[2]]]);
                  IF b[1] THEN WriteLong(1) ELSE WriteLong(0) END;
                  IF b[2] THEN WriteLong(1) ELSE WriteLong(0) END;
                  WriteLn()
                END.
                """);
        assertEquals(" 0 1 1 0\n", out);
    }

    @Test
    @DisplayName("An array element can be passed as a VAR argument, including ReadLong")
    void elementAsVarArgument() {
        String out = LongintTest.printed("""
                VAR a : ARRAY 3 OF INTEGER; v : ARRAY 2 OF LONGINT; i : INTEGER;
                PROCEDURE Inc(VAR n : INTEGER);
                BEGIN
                  n := n + 1
                END Inc;
                BEGIN
                  a[1] := 41;
                  Inc(a[1]); Inc(a[2]);
                  ReadLong(v[1]); ReadLong(v[0]);
                  WriteLong(a[0]); WriteLong(a[1]); WriteLong(a[2]);
                  WriteLong(v[0]); WriteLong(v[1]); WriteLn()
                END.
                """, "123456 -7 ");
        assertEquals(" 0 42 1 -7 123456\n", out);
    }

    @Test
    @DisplayName("An index that is too large stops the program with a message")
    void indexTooLarge() {
        String out = printed("""
                VAR a : ARRAY 3 OF INTEGER; i : INTEGER;
                BEGIN
                  FOR i := 0 TO 3 DO
                    a[i] := i; WriteLong(i)
                  END;
                  WriteLong(99)
                END.
                """);
        assertEquals(" 0 1 2Array index out of range\n", out);
    }

    @Test
    @DisplayName("A negative index stops the program, in a procedure as well")
    void negativeIndex() {
        String out = printed("""
                PROCEDURE P(k : INTEGER);
                VAR a : ARRAY 3 OF INTEGER;
                BEGIN
                  a[0] := 5;
                  WriteLong(a[k]);
                  WriteLong(1)
                END P;
                BEGIN
                  P(0); P(-1); WriteLong(2)
                END.
                """);
        assertEquals(" 5 1Array index out of range\n", out);
    }

    @Test
    @DisplayName("Programs without arrays compile without the range check routine")
    void noRangeRoutineWithoutArrays() {
        String asm = LongintTest.compile("VAR i : INTEGER; BEGIN i := 1 END.");
        assertTrue(!asm.contains("_range"));
    }

    @Test
    @DisplayName("Mistakes with types and arrays are reported")
    void errors() {
        String[] bad = {
                "VAR x : INTEGER; BEGIN x[0] := 1 END.",
                "VAR a : ARRAY 3 OF INTEGER; BEGIN a := 1 END.",
                "VAR a, b : ARRAY 3 OF INTEGER; BEGIN a[0] := b END.",
                "VAR a : ARRAY 3 OF INTEGER; x : INTEGER; BEGIN x := a END.",
                "VAR a : ARRAY 3 OF INTEGER; BEGIN a[0][1] := 1 END.",
                "VAR a : ARRAY 3 OF INTEGER; BEGIN a[TRUE] := 1 END.",
                "VAR a : ARRAY 3 OF INTEGER; BEGIN a[40000] := 1 END.",
                "VAR a : ARRAY 3 OF INTEGER; l : LONGINT; BEGIN a[l] := 1 END.",
                "VAR a : ARRAY 0 OF INTEGER; BEGIN END.",
                "VAR n : INTEGER; a : ARRAY n OF INTEGER; BEGIN END.",
                "VAR a : ARRAY 30000 OF INTEGER; BEGIN END.",
                "VAR a : Missing; BEGIN END.",
                "TYPE T = INTEGER; T = BOOLEAN; BEGIN END.",
                "VAR a : ARRAY 3 OF INTEGER; BEGIN a[0] := TRUE END.",
                "PROCEDURE P(a : ARRAY 3 OF INTEGER); BEGIN END P; BEGIN END." };
        for (String source : bad) {
            assertTrue(hasError(source), source);
        }
    }

    @Test
    @DisplayName("The sieve of Eratosthenes demo prints the primes below 100")
    void sieveDemo() throws java.io.IOException {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of("demo/array-demo.dcl"));
        StringBuilder expected = new StringBuilder();
        for (int p = 2; p < 100; p++) {
            boolean prime = true;
            for (int d = 2; d * d <= p; d++) {
                if (p % d == 0) {
                    prime = false;
                }
            }
            if (prime) {
                expected.append(" ").append(p);
            }
        }
        expected.append("\n");
        assertEquals(expected.toString(), LongintTest.printed(source));
    }
}
