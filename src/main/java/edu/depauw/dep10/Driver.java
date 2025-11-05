package edu.depauw.dep10;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import com.beust.jcommander.JCommander;

public class Driver {
	public static void main(String[] argv) throws IOException {
		Args args = new Args();
		JCommander.newBuilder().addObject(args).build().parse(argv);

		Reader in;

		if (args.parameters.size() > 0) {
			// TODO handle other options and errors
			in = new FileReader(args.parameters.get(0));
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
}
