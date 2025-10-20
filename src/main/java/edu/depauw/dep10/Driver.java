package edu.depauw.dep10;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Driver {
	private Map<Pair<String, Integer>, Macro> macros;

	private List<Line> preprocess(Reader in) throws FileNotFoundException {
		PreprocessContext context = new PreprocessContext(); // TODO preload standard macros
		List<Line> result = new ArrayList<>();

		context.pushReader(in);

		for (var line : context) {
			switch (line) {
			case Line(var label, var command, var args, var _):
				if (command.equalsIgnoreCase(".INCLUDE")) {
					if (args.size() == 1 && args.get(0) instanceof Value.StrLit s) {
						context.pushReader(new FileReader(s.value()));
					}
				} else if (command.equalsIgnoreCase(".DEFMACRO")) {
					if ((args.size() == 1 || args.size() == 2) && args.get(0) instanceof Value.Symbol sym) {
						int numArgs = 0;
						if (args.size() == 2 && args.get(1) instanceof Value.Number n) {
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

					if (!label.isEmpty()) {
						result.add(new Line(label, "", null, ""));
					}
				} else {
					result.add(line);
				}
			}
		}

		return result;
	}

	private void addMacro(Macro macro) {
		macros.put(new Pair<>(macro.name(), macro.numArgs()), macro);
	}

	private Macro getMacro(String name, int numArgs) {
		Macro macro = macros.get(new Pair<>(name, numArgs));
		// TODO check whether it exists
		return macro;
	}

	private void assemble(List<Line> lines) {
		AssembleContext context = new AssembleContext();

		for (var line : lines) {
			switch (line) {
			case Line(var label, var command, var args, var _):
				// Handle .EQUATE separately, because its label is different
				if (command.equalsIgnoreCase(".EQUATE")) {
					context.equate(label, args);
					continue;
				}
			
				if (!label.isEmpty()) {
					context.addLabel(label);
				}
			
				if (command.equalsIgnoreCase(".ALIGN")) {
					context.align(args);
				} else if (command.equalsIgnoreCase(".ASCII")) {
					context.ascii(args);
				} else if (command.equalsIgnoreCase(".BLOCK")) {
					context.block(args);
				} else if (command.equalsIgnoreCase(".BYTE")) {
					context.byt(args);
				} else if (command.equalsIgnoreCase(".ORG")) {
					context.org(args);
				} else if (command.equalsIgnoreCase(".WORD")) {
					context.word(args);
				} else {
					// TODO handle .EXPORT, .IMPORT, .INPUT, .OUTPUT, .SCALL, .SECTION
					context.op(command, args);
				}
			}
		}
		
		// TODO do something with the objects collected in context

		// TODO when compiling OS, relocate according to .ORG and write a "header file"
		// with .EQUATEs for all exported symbols

		// TODO when compiling user file, start at zero (unless .ORG?) by default;
		// preload external symbol table with OS exports

		// TODO extend simulator to load OS and user object files to correct locations
	}

	public static void main(String[] args) throws IOException {
		Reader in;

		if (args.length > 0) {
			// TODO handle options and errors
			in = new FileReader(args[0]);
		} else {
			in = new InputStreamReader(System.in);
		}

		Driver driver = new Driver();
		var lines = driver.preprocess(in); // all macros and includes have been expanded
		in.close();

		driver.assemble(lines);
	}
}
