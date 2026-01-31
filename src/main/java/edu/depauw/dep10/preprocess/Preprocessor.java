package edu.depauw.dep10.preprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.depauw.dep10.Macro;
import edu.depauw.dep10.Pair;
import edu.depauw.dep10.Value;
import edu.depauw.dep10.driver.ErrorLog;

public class Preprocessor {
	private Map<Pair<String, Integer>, Macro> macros;

	public Preprocessor() {
		macros = new HashMap<>();
	}

	public List<Line> preprocess(Sources sources, ErrorLog log) {
		List<Line> result = new ArrayList<>();

		while (sources.hasNext()) {
			var line = sources.next();
			switch (line) {
			case Line(var label, var command, var args, var _, var _):
				if (command.equalsIgnoreCase(".INCLUDE")) {
					if (args.size() == 1 && args.get(0) instanceof Value.StrLit s) {
						sources.pushFile(s.value(), log);
					} else {
					    line.logError("Invalid arguments to .INCLUDE");
					}
				} else if (command.equalsIgnoreCase(".DEFMACRO")) {
					if ((args.size() == 1 || args.size() == 2) && args.get(0) instanceof Value.Symbol sym) {
						int numArgs = 0;
						if (args.size() == 2) {
						    if (args.get(1) instanceof Value.Number n) {
						        numArgs = n.value();
						    } else {
						        line.logError("Expected number of arguments");
						    }
						}

						Macro macro = new Macro(sym.name(), numArgs, sources.extractUntil(".ENDMACRO"));
						addMacro(macro);
					} else {
					    line.logError("Invalid arguments to .DEFMACRO");
					}
				} else if (command.startsWith("@")) {
					String name = command.substring(1);
					int numArgs = args.size();

					Macro macro = getMacro(name, numArgs);
					if (macro != null) {
					    sources.pushLines(command, macro.instantiate(args));
					} else {
					    line.logError("Unknown macro " + command);
					}

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

	private void addMacro(Macro macro) {
		macros.put(new Pair<>(macro.name(), macro.numArgs()), macro);
	}

	private Macro getMacro(String name, int numArgs) {
		Macro macro = macros.get(new Pair<>(name, numArgs));
		return macro;
	}
}
