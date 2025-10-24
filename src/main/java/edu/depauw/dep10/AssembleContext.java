package edu.depauw.dep10;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssembleContext {
	private Map<String, Value> locals;
	private OpTable opTable;

	private int origin;
	private int current;

	private List<String> exports;
	private List<Value> objects;

	public AssembleContext() {
		locals = new HashMap<>();
		opTable = new OpTable();

		origin = 0;
		current = origin;

		exports = new ArrayList<>();
		objects = new ArrayList<>();
	}

	public void addLabel(String label) {
		locals.put(label, new Value.RelativeNumber(current));
	}

	private void addObject(Value value) {
		current += value.size();
		objects.add(value);
	}

	public void equate(String label, List<Value> args) {
		checkOneArg(args);

		locals.put(label, args.get(0));
	}

	public void align(List<Value> args) {
		checkOneArg(args);

		switch (args.get(0)) {
		case Value.Number(var n):
			if (n == 1 || n == 2 || n == 4 || n == 8) {
				if (current % n != 0) {
					int skip = n - (current % n);
					addObject(new Value.Block(skip));
				}
			} else {
				// TODO error
			}
			break;
		default:
			// TODO error
		}
	}

	public void ascii(List<Value> args) {
		checkOneArg(args);

		var arg = args.get(0);
		if (arg instanceof Value.StrLit) {
			addObject(arg);
		} else {
			addObject(new Value.LowByte(arg));
		}
	}

	public void block(List<Value> args) {
		checkOneArg(args);

		switch (args.get(0)) {
		case Value.Number(var n):
			if (n < 0) {
				// TODO error
			} else {
				addObject(new Value.Block(n));
			}
			break;
		default:
			// TODO error
		}
	}

	public void byt(List<Value> args) {
		checkOneArg(args);

		var arg = args.get(0);
		addObject(new Value.LowByte(arg));
	}

	public void export(List<Value> args) {
		checkOneArg(args);

		var arg = args.get(0);
		if (arg instanceof Value.Symbol(var sym)) {
			exports.add(sym);
		} else {
			// TODO error
		}
	}

	public void org(List<Value> args) {
		checkOneArg(args);

		switch (args.get(0)) {
		case Value.Number(var n):
			origin = n - current;
			break;
		default:
			// TODO error
		}
	}

	public void word(List<Value> args) {
		checkOneArg(args);

		var arg = args.get(0);
		if (arg instanceof Value.CharLit(var c)) {
			addObject(new Value.Number(c));
		} else {
			addObject(arg);
		}
	}

	public void op(String command, List<Value> args) {
		String mode = "";

		if (args.size() == 1) {
			mode = "i";
		} else if (args.size() == 2) {
			if (args.get(1) instanceof Value.Symbol(var sym)) {
				mode = sym;
			} else {
				// TODO error
			}
		} else if (args.size() > 2) {
			// TODO error
		}

		var opcode = opTable.lookup(command, mode);

		addObject(new Value.LowByte(new Value.Number(opcode)));

		var op = opTable.get(opcode);
		if (op.hasOperand()) {
			var arg = args.get(0);
			if (arg instanceof Value.CharLit(var c)) {
				addObject(new Value.Number(c));
			} else {
				addObject(arg);
			}
		}
	}

	public Result getResult() {
		Result result = new Result(origin);

		for (Value value : objects) {
			switch (value) {
			case Value.Block(var size): {
				for (int i = 0; i < size; i++) {
					result.add(new UByte(0));
				}
				break;
			}

			case Value.CharLit(var c): {
				result.add(new UByte(c));
				break;
			}

			case Value.LowByte(var v): {
				var w = evaluate(v);
				result.add(w.lo());
				break;
			}

			case Value.StrLit(var s): {
				for (int i = 0; i < s.length(); i++) {
					result.add(new UByte(s.charAt(i)));
				}
				break;
			}

			default: {
				var w = evaluate(value);
				result.add(w.hi());
				result.add(w.lo());
				break;
			}
			}
		}

		return result;
	}

	private Word evaluate(Value v) {
		switch (v) {
		case Value.CharLit(var c):
			return new Word(c);
		case Value.Number(var n):
			return new Word(n);
		case Value.RelativeNumber(var n):
			return new Word(n + origin);
		case Value.StrLit(var s):
			if (s.length() == 1) {
				return new Word(s.charAt(0));
			} else if (s.length() == 2) {
				return new Word(s.charAt(0) * 256 + s.charAt(1));
			} else {
				// TODO error
				return null;
			}
		case Value.Symbol(var sym):
			if (locals.containsKey(sym)) {
				return evaluate(locals.get(sym));
			} else {
				// TODO check globals, or error
			}
			break;
		default:
			// TODO error
		}
		return null;
	}

	private void checkOneArg(List<Value> args) {
		if (args.size() != 1) {
			// TODO error
		}
	}
}
