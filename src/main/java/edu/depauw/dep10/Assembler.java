package edu.depauw.dep10;

import java.util.List;

import edu.depauw.dep10.driver.ErrorLog;
import edu.depauw.dep10.preprocess.Line;

public class Assembler {
    private ErrorLog log;
    private Result result;
    
	private OpTable opTable;

	public Assembler(ErrorLog log) {
	    this.log = log;
	    this.result = new Result();
	    
	    this.opTable = new OpTable();
	}

	public Result assemble(List<Line> lines) {
		for (var line : lines) {
		    result.addLine(line);
		    
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
                		result.addLabel(label);
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
                	} else if (command.equalsIgnoreCase(".SECTION")) {
                	    section(args);
                	} else if (command.equalsIgnoreCase(".WORD")) {
                		word(args);
                	} else if (command.startsWith(".")) {
                		// Ignore other directives: .IMPORT, .INPUT, .OUTPUT, .SCALL
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
		
		// Now pack the sections in the Result and resolve the relative addresses
		result.resolveObjects();
		
		return result;
		
		// TODO extend simulator to load OS and user sections from object file to correct locations
	}

	private void equate(String label, List<Value> args) throws LineError {
		checkOneArg(args);
		result.equate(label, args.get(0));
	}

	private void align(List<Value> args) throws LineError {
		checkOneArg(args);

		switch (args.get(0)) {
		case Value.Number(var n):
			if (n == 1 || n == 2 || n == 4 || n == 8) {
			    result.align(n);
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
			result.addObject(arg);
		} else {
			result.addObject(new Value.LowByte(arg));
		}
	}

	private void block(List<Value> args) throws LineError {
		checkOneArg(args);

		switch (args.get(0)) {
		case Value.Number(var n):
			if (n < 0) {
				throw new LineError("Block size can not be negative");
			} else {
				result.addObject(new Value.Block(n));
			}
			break;
		default:
			throw new LineError("Block size not a number");
		}
	}

	private void byt(List<Value> args) throws LineError {
		checkOneArg(args);

		var arg = args.get(0);
		result.addObject(new Value.LowByte(arg));
	}

	private void export(List<Value> args) throws LineError {
		checkOneArg(args);

		var arg = args.get(0);
		if (arg instanceof Value.Symbol(var sym)) {
		    result.addGlobal(sym);
		} else {
			throw new LineError("Not a symbol");
		}
	}

	private void org(List<Value> args) throws LineError {
		checkOneArg(args);

		switch (args.get(0)) {
		case Value.Number(var n):
			result.org(n);
			break;
		default:
			throw new LineError("Address not a number");
		}
	}
	
	private void section(List<Value> args) throws LineError {
	    // TODO
	}

	private void word(List<Value> args) throws LineError {
		checkOneArg(args);

		var arg = args.get(0);
		if (arg instanceof Value.CharLit(var c)) {
			result.addObject(new Value.Number(c));
		} else {
			result.addObject(arg);
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
		if (opcode < 0) {
		    throw new LineError("Unrecognized opcode: " + command);
		}

		result.addObject(new Value.LowByte(new Value.Number(opcode)));

		var op = opTable.get(opcode);
		if (op.hasOperand()) {
			var arg = args.get(0);
			if (arg instanceof Value.CharLit(var c)) {
				result.addObject(new Value.Number(c));
			} else {
				result.addObject(arg);
			}
		}
	}

	private void checkOneArg(List<Value> args) throws LineError {
		if (args.size() != 1) {
			throw new LineError("Expected one argument");
		}
	}
}
