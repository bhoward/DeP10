package edu.depauw.dep10;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.depauw.dep10.driver.ErrorLog;
import edu.depauw.dep10.preprocess.Line;

public class Assembler {
	private Map<String, Value> locals;
	private OpTable opTable;

	private int origin;
	private int current;

	private List<String> exports;
	private List<Value> objects;

	public Assembler() {
		locals = new HashMap<>();
		opTable = new OpTable();

		origin = 0;
		current = origin;

		exports = new ArrayList<>();
		objects = new ArrayList<>();
	}

	public Result assemble(List<Line> lines, ErrorLog log) {
		for (var line : lines) {
			try {
                switch (line) {
                case Line(var label, var command, var args, var _, var _):
                    if (line.isCommented()) {
                        continue;
                    }
                
                	// Handle .EQUATE separately, because its label is different
                	if (command.equalsIgnoreCase(".EQUATE")) {
                		equate(label, args);
                		continue;
                	}
                
                	if (!label.isEmpty()) {
                		addLabel(label);
                	}
                
                	if (command.equalsIgnoreCase(".ALIGN")) {
                		align(args);
                	} else if (command.equalsIgnoreCase(".ASCII")) {
                		ascii(args);
                	} else if (command.equalsIgnoreCase(".BLOCK")) {
                		block(args);
                	} else if (command.equalsIgnoreCase(".BYTE")) {
                		byt(args);
                	} else if (command.equalsIgnoreCase(".EXPORT")) {
                		export(args);
                	} else if (command.equalsIgnoreCase(".ORG")) {
                		org(args);
                	} else if (command.equalsIgnoreCase(".WORD")) {
                		word(args);
                	} else if (command.startsWith(".")) {
                		// Ignore other directives: .IMPORT, .INPUT, .OUTPUT, .SCALL, .SECTION
                		// TODO at least diagnose errors with them...
                	} else if (command.isEmpty()) {
                		// Skip this line, except for the listing
                	    if (args != null && !args.isEmpty()) {
                	        line.logError("Arguments present with no command");
                	    }
                	} else {
                		op(command, args);
                	}
                }
            } catch (LineError e) {
                line.logError(e.getMessage());
            }
		}
		
		return getResult(log);
		
		// TODO when compiling OS, relocate according to .ORG and write a "header file"
		// with .EQUATEs for all exported symbols

		// TODO when compiling user file, start at zero (unless .ORG?) by default;
		// preload external symbol table with OS exports

		// TODO extend simulator to load OS and user object files to correct locations
	}

	private void addLabel(String label) {
		locals.put(label, new Value.RelativeNumber(current));
	}

	private void addObject(Value value) {
		current += value.size();
		objects.add(value);
	}

	private void equate(String label, List<Value> args) throws LineError {
		checkOneArg(args);

		locals.put(label, args.get(0));
	}

	private void align(List<Value> args) throws LineError {
		checkOneArg(args);

		switch (args.get(0)) {
		case Value.Number(var n):
			if (n == 1 || n == 2 || n == 4 || n == 8) {
				if (current % n != 0) {
					int skip = n - (current % n);
					addObject(new Value.Block(skip));
				}
			} else {
				throw new LineError("Invalid alignment size");
			}
			break;
		default:
			throw new LineError("Alignment size not a number");
		}
	}

	private void ascii(List<Value> args) throws LineError {
		checkOneArg(args);

		var arg = args.get(0);
		if (arg instanceof Value.StrLit) {
			addObject(arg);
		} else {
			addObject(new Value.LowByte(arg));
		}
	}

	private void block(List<Value> args) throws LineError {
		checkOneArg(args);

		switch (args.get(0)) {
		case Value.Number(var n):
			if (n < 0) {
				throw new LineError("Block size can not be negative");
			} else {
				addObject(new Value.Block(n));
			}
			break;
		default:
			throw new LineError("Block size not a number");
		}
	}

	private void byt(List<Value> args) throws LineError {
		checkOneArg(args);

		var arg = args.get(0);
		addObject(new Value.LowByte(arg));
	}

	private void export(List<Value> args) throws LineError {
		checkOneArg(args);

		var arg = args.get(0);
		if (arg instanceof Value.Symbol(var sym)) {
			exports.add(sym);
		} else {
			throw new LineError("Not a symbol");
		}
	}

	private void org(List<Value> args) throws LineError {
		checkOneArg(args);

		switch (args.get(0)) {
		case Value.Number(var n):
			origin = n - current;
			break;
		default:
			throw new LineError("Address not a number");
		}
	}

	private void word(List<Value> args) throws LineError {
		checkOneArg(args);

		var arg = args.get(0);
		if (arg instanceof Value.CharLit(var c)) {
			addObject(new Value.Number(c));
		} else {
			addObject(arg);
		}
	}

	private void op(String command, List<Value> args) throws LineError {
		String mode = "";

		if (args.size() == 1) {
			mode = "i";
		} else if (args.size() == 2) {
			if (args.get(1) instanceof Value.Symbol(var sym)) {
				mode = sym;
			} else {
				throw new LineError("Unrecognized mode");
			}
		} else if (args.size() > 2) {
			throw new LineError("Too many arguments");
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

	private Result getResult(ErrorLog log) {
		Result result = new Result(origin);

		for (Value value : objects) {
			try {
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
            } catch (ValueError e) {
                log.error(e.getMessage());
            }
		}

		return result;
	}

	private Word evaluate(Value v) throws ValueError {
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
				throw new ValueError("String needs to be one or two characters: \"" + s + "\"");
			}
		case Value.Symbol(var sym):
			if (locals.containsKey(sym)) {
				return evaluate(locals.get(sym));
			} else {
				// TODO check globals, or error
			}
			break;
		default:
		    // This shouldn't happen
			throw new RuntimeException("Unrecognized value");
		}
		return null;
	}

	private void checkOneArg(List<Value> args) throws LineError {
		if (args.size() != 1) {
			throw new LineError("Expected one argument");
		}
	}
}
