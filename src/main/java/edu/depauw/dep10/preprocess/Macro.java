package edu.depauw.dep10.preprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.depauw.dep10.LineError;
import edu.depauw.dep10.Value;

public record Macro(String name, int numArgs, List<Line> body) {
	public List<Line> instantiate(List<Value> actual) {
		List<Line> result = new ArrayList<>();
		Map<String, Value> argMap = new HashMap<>();

		for (int i = 0; i < numArgs; i++) {
			var param = "$" + (i + 1);
			if (i < actual.size()) {
				argMap.put(param, actual.get(i));
			} else {
				// Leave unsupplied parameters unchanged
				argMap.put(param, new Value.StrLit(param));
			}
		}

		for (var line : body) {
			try {
                switch (line) {
                case Line(var label, var command, var args, var comment, var _):
                	label = substString(label, argMap);

                	args = args.stream().map(arg -> substValue(arg, argMap)).toList();

                	result.add(Line.of(label, command, args, comment));
                }
            } catch (LineError e) {
                line.logError(e.getMessage());
            }
		}

		return result;
	}

	private Value substValue(Value v, Map<String, Value> argMap) {
		switch (v) {
		case Value.Symbol(var sym):
			if (sym.startsWith("$")) {
				return argMap.computeIfAbsent(sym, Macro::genSym);
			} else {
				return v;
			}
		default:
			return v;
		}
	}

	private String substString(String s, Map<String, Value> argMap) throws LineError {
		if (s.startsWith("$")) {
			var replacement = argMap.computeIfAbsent(s, Macro::genSym);

			if (replacement instanceof Value.Symbol(var sym)) {
				return sym;
			} else {
				throw new LineError("Macro argument not a symbol");
			}
		}
		return s;
	}

	private static int sequenceNumber = 0;
	
	private static Value genSym(String s) {
		return new Value.Symbol("_" + (sequenceNumber++));
	}
	
	public static void main(String[] args) {
		List<Line> body = List.of(
				Line.of("", "LINE1", List.of(new Value.Symbol("$1"), new Value.Symbol("i")), ""),
				Line.of("$3", "LINE2", List.of(new Value.Symbol("$2")), ""),
				Line.of("", "LINE3", List.of(new Value.Symbol("$3")), "")
				);
		Macro test = new Macro("TEST", 2, body);
		System.out.println(test.instantiate(List.of(new Value.Number(42))));
	}
}
