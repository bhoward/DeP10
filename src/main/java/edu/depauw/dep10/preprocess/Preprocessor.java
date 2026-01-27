package edu.depauw.dep10.preprocess;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import edu.depauw.dep10.Macro;
import edu.depauw.dep10.Pair;
import edu.depauw.dep10.Parser;
import edu.depauw.dep10.Value;

public class Preprocessor {
	private Map<Pair<String, Integer>, Macro> macros;
	private Stack<Iterator<Line>> stack;
	private Iterator<Line> lines;

	public Preprocessor() {
		macros = new HashMap<>();
		stack = new Stack<>();
		lines = new Iterator<>() {
			public boolean hasNext() {
				while (!stack.isEmpty() && !stack.peek().hasNext()) {
					stack.pop();
				}

				return !stack.isEmpty();
			}

			public Line next() {
				// Precondition: hasNext() is true
				return stack.peek().next();
			}
		};
	}

	public List<Line> preprocess(Reader in) throws FileNotFoundException {
		// TODO preload standard macros
		List<Line> result = new ArrayList<>();

		pushReader(in);

		while (lines.hasNext()) {
			var line = lines.next();
			switch (line) {
			case Line(var label, var command, var args, var _, var _):
				if (command.equalsIgnoreCase(".INCLUDE")) {
					if (args.size() == 1 && args.get(0) instanceof Value.StrLit s) {
						pushReader(new FileReader(s.value()));
					}
				} else if (command.equalsIgnoreCase(".DEFMACRO")) {
					if ((args.size() == 1 || args.size() == 2) && args.get(0) instanceof Value.Symbol sym) {
						int numArgs = 0;
						if (args.size() == 2 && args.get(1) instanceof Value.Number n) {
							numArgs = n.value();
						}

						Macro macro = new Macro(sym.name(), numArgs, extractUntil(".ENDMACRO"));
						addMacro(macro);
					}
				} else if (command.startsWith("@")) {
					String name = command.substring(1);
					int numArgs = args.size();

					Macro macro = getMacro(name, numArgs);
					pushLines(macro.instantiate(args));

					if (!label.isEmpty()) {
						result.add(Line.of(label, "", null, ""));
					}
				} else {
					result.add(line);
				}
			}
		}

		return result;
	}

	// Note that these three methods affect the underlying iterators that supply
	// the lines iterator!
	private void pushReader(Reader in) {
		stack.push(Parser.parse(in).iterator());
	}

	private void pushLines(List<Line> lines) {
		stack.push(lines.iterator());
	}

	private List<Line> extractUntil(String end) {
		List<Line> result = new ArrayList<>();

		var it = stack.peek();
		while (it.hasNext()) {
			var line = it.next();
			if (line.command().equalsIgnoreCase(end)) {
				break;
			} else {
				result.add(line);
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
}
