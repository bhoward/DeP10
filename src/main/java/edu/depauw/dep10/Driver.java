package edu.depauw.dep10;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import com.beust.jcommander.JCommander;

public class Driver {
    public static void main(String[] argv) throws IOException {
        InitialArgs init = new InitialArgs();
        CommandAsm asm = new CommandAsm();
        CommandRun run = new CommandRun();
        JCommander jc = JCommander.newBuilder()
                .addObject(init)
                .addCommand("asm", asm)
                .addCommand("run", run)
                .build();
        jc.parse(argv);

        if (init.showHelp) {
            jc.usage();
        } else {
            switch (jc.getParsedCommand()) {
            case "asm":
                if (asm.showHelp) {
                    jc.usage("asm");
                } else {
                    doAsm(asm);
                }
                break;
            case "run":
                if (run.showHelp) {
                    jc.usage("run");
                } else {
                    doRun(run);
                }
                break;
            }
        }
    }

    private static void doAsm(CommandAsm asm) throws FileNotFoundException, IOException {
        if (asm.sourceList.size() > 1 || (asm.sourceList.size() > 0 && asm.sourceFile != null)) {
            System.err.println("Only one source file allowed.");
            System.exit(1);
        }
        
        if (asm.bareMetal && asm.osName != null) {
            System.err.println("Bare metal excludes specifying OS.");
            System.exit(1);
        }
        
        Reader in;

        if (asm.sourceList.size() > 0) {
            // TODO handle errors
            in = new FileReader(asm.sourceList.get(0));
        } else  if (asm.sourceFile != null) {
            in = new FileReader(asm.sourceFile);
        } else {
            in = new InputStreamReader(System.in);
        }

        var preprocessor = new Preprocessor();
        var lines = preprocessor.preprocess(in); // all macros and includes have been expanded
        in.close();

        var assembler = new Assembler();
        Result result = assembler.assemble(lines);
        System.out.println(result);
    }

    private static void doRun(CommandRun run) {

    }
}
