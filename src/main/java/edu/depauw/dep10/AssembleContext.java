package edu.depauw.dep10;

import java.util.ArrayList;
import java.util.List;

public class AssembleContext {
	private SymTable locals;
	private OpTable opTable;
	private int origin;
	private int current;

	private List<Value> objects;

	public AssembleContext() {
		locals = new SymTable();
		opTable = new OpTable();
		origin = 0;
		current = origin;

		objects = new ArrayList<>();
	}

	public void addLabel(String label) {
		locals.add(label, new Value.Number(current));
	}

	public void equate(String label, List<Value> args) {
		checkOneArg(args);

		locals.add(label, args.get(0));
	}

	public void align(List<Value> args) {
		checkOneArg(args);

		switch (args.get(0)) {
		case Value.Number(var n):
			if (n == 1 || n == 2 || n == 4 || n == 8) {
				if (current % n != 0) {
					int skip = n - (current % n);
					current += skip;
					objects.add(new Value.Block(skip));
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
		if (arg instanceof Value.StrLit(var s)) {
			current += s.length();
			objects.add(arg);
		} else {
			objects.add(new Value.LowByte(arg));
		}
	}

	public void block(List<Value> args) {
		checkOneArg(args);

		switch (args.get(0)) {
		case Value.Number(var n):
			if (current < 0) {
				// TODO error
			} else {
				current += n;
				objects.add(new Value.Block(n));
			}
			break;
		default:
			// TODO error
		}
	}

	public void byt(List<Value> args) {
		checkOneArg(args);

		var arg = args.get(0);
		current += 1;
		objects.add(new Value.LowByte(arg));
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
		current += 2;
		objects.add(arg);
	}

	public void op(String command, List<Value> args) {
		checkTwoArgs(args);

		switch (args.get(1)) {
		case Value.Symbol(var mode):
			var opcode = opTable.lookup(command, mode);
			
			current += 1;
			objects.add(new Value.LowByte(new Value.Number(opcode)));
			
			var op = opTable.get(opcode);
			if (op.hasOperand()) {
				current += 2;
				objects.add(args.get(0));
			}
			break;
		default:
			// TODO error
		}
	}

	private void checkOneArg(List<Value> args) {
		if (args.size() != 1) {
			// TODO error
		}
	}

	private void checkTwoArgs(List<Value> args) {
		if (args.size() != 2) {
			// TODO error
		}
	}
}
