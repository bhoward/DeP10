package edu.depauw.declan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import edu.depauw.declan.ast.Program;
import edu.depauw.declan.ir.Instruction;
import edu.depauw.declan.resolved.RProg;

public class DeCLan {
    private static final String SAMPLE = """
            CONST three = 3; seven = 7;
            VAR answer : INTEGER;
            PROCEDURE gcd(a, b: INTEGER; VAR result: INTEGER);
              VAR m, n : INTEGER;
              BEGIN
                m := a;
                n := b;
                WHILE m # n DO
                  IF m > n THEN m := m - n ELSE n := n - m END
                END;
                result := m
              END gcd;
            PROCEDURE fact(n: INTEGER; VAR factn: INTEGER);
              VAR factnm1 : INTEGER;
              BEGIN
                IF n = 0 THEN factn := 1
                ELSE fact(n - 1, factnm1); factn := n * factnm1;
                END
              END fact;
            BEGIN
              fact(three, answer);
              gcd(answer, seven, answer);
              answer := three * seven * (answer + answer);
              WriteInt(answer);
              WriteLn()
            END. (* Don't forget the ending period! *)
            """;

    public static void main(String[] args) throws IOException {
        var reporter = new Reporter(System.err);
        if (args.length > 1) {
            System.out.println("Usage: declan [file or -]");
            System.exit(64); // [64]
        } else if (args.length == 1) {
            if (args[0].equals("-")) {
                System.out.print(runStdin(reporter));
            } else {
                System.out.print(runFile(args[0], reporter));
            }
        } else {
            System.out.print(run(SAMPLE, reporter));
        }
    }

    private static String runFile(String path, Reporter reporter) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String result = run(new String(bytes, Charset.defaultCharset()), reporter);

        // Indicate an error in the exit code.
        if (reporter.hadError())
            System.exit(65);
        
        return result;
    }

    private static String runStdin(Reporter reporter) throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        StringBuilder source = new StringBuilder();

        String line = reader.readLine();
        while (line != null) {
            source.append(line);
            source.append('\n');
            line = reader.readLine();
        }

        return run(source.toString(), reporter);
    }

    public static String run(String source, Reporter reporter) {
        reporter.reset();
        Scanner scanner = new Scanner(source, reporter);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens, reporter);
        Program program = parser.parse();

//        System.out.println(AstPrettyPrinter.print(program, 100));

        RProg prog = TypeChecker.check(program, reporter, false);
        StringBuilder out = new StringBuilder();
        if (!reporter.hadError()) {
//            Interpreter.run(program, false);
//            Interpreter2.run(prog, false);

            List<Instruction> instructions = Generator.generate(prog);
//            for (Instruction instr : instructions) {
//                System.out.println("; " + instr);
//            }

            for (String s : Pep10.translate(instructions)) {
                out.append(s + "\n");
            }
        }
        
        return out.toString(); // TODO report the errors somehow!
    }
}
