package edu.depauw.dep10.driver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.beust.jcommander.JCommander;

import edu.depauw.dep10.Assembler;
import edu.depauw.dep10.Result;
import edu.depauw.dep10.preprocess.Preprocessor;
import edu.depauw.dep10.preprocess.Sources;

public class Driver {
	public static void main(String[] argv) throws IOException {
		ErrorLog log = new ErrorLog();

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
					doAsm(asm, log);
				}
				break;
			case "run":
				if (run.showHelp) {
					jc.usage("run");
				} else {
					doRun(run, log);
				}
				break;
			}
		}
		
		// TODO Check log for errors and print them here
	}

	private static void doAsm(CommandAsm asm, ErrorLog log) {
	    Sources sources = new Sources();
	    
	    // First add standard macros
	    sources.addResource("/stdmacro.pep", log); // TODO constant for the name; option to substitute?
	    
	    // Next add the distinguished source file, if any
	    if (asm.sourceFile != null) {
	        sources.addFile(asm.sourceFile, log);
	    }
	    
	    // Add any other source files from the command line
	    for (var name : asm.sourceList) {
	        sources.addFile(name, log);
	    }
	    
	    // If no sources specified, add standard input
	    if (asm.sourceFile == null && asm.sourceList.isEmpty()) {
	        sources.addStdIn();
	    }
	    
		// TODO open the appropriate OS file
		if (asm.bareMetal && asm.osName != null) {
			log.error("Bare metal excludes specifying OS.");
			return;
		}

		var preprocessor = new Preprocessor();
		var lines = preprocessor.preprocess(sources, log); // all macros and includes have been expanded
		
		var assembler = new Assembler();
		Result result = assembler.assemble(lines, log);
		
		if (asm.objectFile != null) {
		    try (var out = new BufferedWriter(new FileWriter(asm.objectFile))) {
		        out.write(result.toString());
		    } catch (IOException e) {
                log.error(e.getMessage());
            }
		} else {
		    System.out.println(result);
		}
		
		if (asm.listingFile != null) {
		    try (var out = new PrintWriter(new BufferedWriter(new FileWriter(asm.listingFile)))) {
		        for (var line : lines) {
		            if (line.isVisible()) {
		                out.println(line);
		            }
		        }
		    } catch (IOException e) {
		        log.error(e.getMessage());
		    }
		}
		
		if (asm.errorFile != null) {
		    // TODO
		}
	}
	
	private static void doRun(CommandRun run, ErrorLog log) {
	    // TODO
	}
}
