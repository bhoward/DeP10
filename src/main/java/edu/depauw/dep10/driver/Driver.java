package edu.depauw.dep10.driver;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;

import edu.depauw.dep10.Assembler;
import edu.depauw.dep10.Result;
import edu.depauw.dep10.preprocess.Preprocessor;

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
		List<String> sources = new ArrayList<>(asm.sourceList);
		if (asm.sourceFile != null) {
			sources.add(asm.sourceFile);
		}

		// TODO handle more than one source file?
		String sourceName = null;
		if (sources.size() > 1) {
			log.error("Only one source file allowed.");
			return;
		} else if (sources.size() == 1) {
			sourceName = sources.getFirst();
		}

		// TODO pass this information along to the assembler...
		if (asm.bareMetal && asm.osName != null) {
			log.error("Bare metal excludes specifying OS.");
			return;
		}

		try (Reader in = openSource(sourceName)) {
			var preprocessor = new Preprocessor();
			var lines = preprocessor.preprocess(in); // all macros and includes have been expanded
			
			var assembler = new Assembler();
			Result result = assembler.assemble(lines);
			
			System.out.println(result);
		} catch (IOException e) {
			log.error(e.getMessage());
			return;
		}
	}

	/**
	 * Open a Reader for the given sourceName, or System.in if name is null.
	 * 
	 * @param sourceName
	 * @return
	 * @throws FileNotFoundException
	 */
	private static Reader openSource(String sourceName) throws FileNotFoundException {
		if (sourceName != null) {
			return new FileReader(sourceName);
		} else {
			return new InputStreamReader(System.in);
		}
	}

	private static void doRun(CommandRun run, ErrorLog log) {

	}
}
