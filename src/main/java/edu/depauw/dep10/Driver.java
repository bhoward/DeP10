package edu.depauw.dep10;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import com.beust.jcommander.JCommander;

public class Driver {
	public static void main(String[] argv) throws IOException {
		CommandAsm asm = new CommandAsm();
		CommandRun run = new CommandRun();
		JCommander jc = JCommander.newBuilder()
				.addCommand("asm", asm)
				.addCommand("run", run)
				.build();
		jc.parse(argv);

		switch (jc.getParsedCommand()) {
		case "asm":
			Reader in;
			
			if (asm.parameters.size() > 0) {
				// TODO handle other options and errors
				in = new FileReader(asm.parameters.get(0));
			} else {
				in = new InputStreamReader(System.in);
			}

			var preprocessor = new Preprocessor();
			var lines = preprocessor.preprocess(in); // all macros and includes have been expanded
			in.close();

			var assembler = new Assembler();
			Result result = assembler.assemble(lines);
			System.out.println(result);
			break;
		case "run":
			break;
		}
	}
}
