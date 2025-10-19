package edu.depauw.dep10;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class Driver {
	public static void main(String[] args) throws IOException {
		Reader in;

		if (args.length > 0) {
			// TODO handle options and errors
			in = new FileReader(args[0]);
		} else {
			in = new InputStreamReader(System.in);
		}

		var lines = preprocess(in); // all macros and includes have been expanded
		in.close();

		assemble(lines);
	}

	private static List<Line> preprocess(Reader in) throws FileNotFoundException {
		PreprocessContext context = new PreprocessContext(); // TODO preload standard macros
		List<Line> result = new ArrayList<>();

		context.pushReader(in);

		for (var line : context) {
			switch (line) {
			case Line(var _, var command, var args, var _):
				if (command.equalsIgnoreCase(".INCLUDE")) {
					if (args.size() == 1 && args.get(0) instanceof Arg.StrLit s) {
						context.pushReader(new FileReader(s.value()));
					}
				} else if (command.equalsIgnoreCase(".DEFMACRO")) {
					if ((args.size() == 1 || args.size() == 2) && args.get(0) instanceof Arg.Symbol sym) {
						int numArgs = 0;
						if (args.size() == 2 && args.get(1) instanceof Arg.Number n) {
							numArgs = n.value();
						}

						Macro macro = new Macro(sym.name(), numArgs, context.extractUntil(".ENDMACRO"));
						addMacro(macro);
					}
				} else if (command.startsWith("@")) {
					String name = command.substring(1);
					int numArgs = args.size();

					Macro macro = getMacro(name, numArgs);
					context.pushLines(macro.instantiate(args));
				} else {
					result.add(line);
				}
			}
		}

		return result;
	}

	private static void addMacro(Macro macro) {
		// TODO Auto-generated method stub

	}

	private static Macro getMacro(String name, int numArgs) {
		// TODO Auto-generated method stub
		return null;
	}

	private static void assemble(List<Line> lines) {
		// TODO one pass to check for errors and compute relative symbol locations, then a
		// second to generate object file
		
		// TODO when compiling OS, relocate according to .ORG and write a "header file"
		// with .EQUATEs for all exported symbols
		
		// TODO when compiling user file, start at zero (unless .ORG?) by default;
		// preload external symbol table with OS exports
		
		// TODO extend simulator to load OS and user object files to correct locations
	}
}
