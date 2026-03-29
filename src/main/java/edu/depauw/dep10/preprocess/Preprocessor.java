package edu.depauw.dep10.preprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.depauw.dep10.assemble.Value;
import edu.depauw.dep10.driver.ErrorLog;
import edu.depauw.dep10.util.Pair;

public class Preprocessor {
    private ErrorLog log;
	private Map<Pair<String, Integer>, Macro> macros;

	public Preprocessor(ErrorLog log) {
	    this.log = log;
		macros = new HashMap<>();
	}

	public List<Line> preprocess(Sources sources) {
		List<Line> result = new ArrayList<>();

		while (sources.hasNext()) {
			var line = sources.next();
			switch (line) {
			case Line(var _, var command, var args, var _, var _):
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
				} else if (command.equalsIgnoreCase(".SCALL")) {
				    if (args.size() == 1 && args.get(0) instanceof Value.Symbol sym) {
				        var body = new ArrayList<Line>();
				        body.add(Line.of("", "LDWA", List.of(sym, Value.fromString("i")), ""));
				        body.add(Line.of("", "SCALL", List.of(Value.fromString("$1"), Value.fromString("$2")), ""));
				        Macro macro = new Macro(sym.name(), 2, body);
				        addMacro(macro);
				    } else {
				        line.logError("Invalid arguments to .SCALL");
				    }
				} else {
					result.add(line);
				}
			}
		}

		return expandMacros(result);
	}

	private List<Line> expandMacros(List<Line> lines) {
        List<Line> result = new ArrayList<>();
        
	    Sources sources = new Sources();
	    sources.pushAll(lines);
	    
	    while (sources.hasNext()) {
	        var line = sources.next();
	        switch (line) {
            case Line(var _, var command, var args, var _, var _):
                if (command.startsWith("@")) {
                  line.setComment();
                  
                  String name = command.substring(1);
                  int numArgs = args.size();

                  Macro macro = getMacro(name, numArgs);
                  if (macro != null) {
                      sources.pushLines(command, macro.instantiate(args));
                  } else {
                      line.logError("Unknown macro " + command);
                  }

                  result.add(line);
                } else {
                    result.add(line);
                }
	        }
	    }
	    
	    return result;
	}
	
	private void addMacro(Macro macro) {
		macros.put(new Pair<>(macro.name().toUpperCase(), macro.numArgs()), macro);
	}

	private Macro getMacro(String name, int numArgs) {
		Macro macro = macros.get(new Pair<>(name.toUpperCase(), numArgs));
		return macro;
	}
}
