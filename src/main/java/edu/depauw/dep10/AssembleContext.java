package edu.depauw.dep10;

import java.util.ArrayList;
import java.util.List;

public class AssembleContext {
	private SymTable locals;
	private OpTable opTable;
	private int origin;
	private int current;

	private List<ObjectItem> objects;

	private interface ObjectItem {
	}

	private record BlockItem(int size) implements ObjectItem {
	}

	private record ByteItem(UByte b) implements ObjectItem {
	}

	private record WordItem(Word w) implements ObjectItem {
	}

	private record StringItem(String s) implements ObjectItem {
	}

	private record SymbolItem(String name) implements ObjectItem {
	}

	public AssembleContext() {
		locals = new SymTable();
		opTable = new OpTable();
		origin = 0;
		current = origin;

		objects = new ArrayList<>();
	}

	public void addLabel(String label) {
		locals.add(label, current);
	}

	public void equate(String label, List<Arg> args) {
		checkOneArg(args);

		int value = getValue(args.get(0));
		// TODO handle error
		locals.add(label,  value);
	}

	private int getValue(Arg arg) {
		switch (arg) {
		case Arg.Number(var n):
			return n;
		case Arg.StrLit(var s):
			if (s.length() == 1) {
				return s.charAt(0);
			} else if (s.length() == 2) {
				return s.charAt(0) * 256 + s.charAt(1);
			} else {
				// TODO error
			}
			break;
		case Arg.CharLit(var c):
			return c;
		default:
			// TODO error
		}
		return 0;
	}

	public void align(List<Arg> args) {
		checkOneArg(args);

		switch (args.get(0)) {
		case Arg.Number(var n):
			if (n == 1 || n == 2 || n == 4 || n == 8) {
				if (current % n != 0) {
					int skip = n - (current % n);
					current += skip;
					objects.add(new BlockItem(skip));
				}
			} else {
				// TODO error
			}
			break;
		default:
			// TODO error
		}
	}

	public void ascii(List<Arg> args) {
		checkOneArg(args);

		switch (args.get(0)) {
		case Arg.StrLit(var s):
			current += s.length();
			objects.add(new StringItem(s));
			break;
		case Arg.CharLit(var _):
			current += 1;
			break;
		default:
			// TODO error
		}
	}

	public void block(List<Arg> args) {
		checkOneArg(args);

		switch (args.get(0)) {
		case Arg.Number(var n):
			if (current < 0) {
				// TODO error
			} else {
				current += n;
				objects.add(new BlockItem(n));
			}
			break;
		default:
			// TODO error
		}
	}

	public void byt(List<Arg> args) {
		checkOneArg(args);

		int value = getValue(args.get(0));
		// TODO handle error
		current += 1;
		objects.add(new ByteItem(new UByte(value)));
	}

	public void org(List<Arg> args) {
		checkOneArg(args);

		switch (args.get(0)) {
		case Arg.Number(var n):
			origin = n - current;
			break;
		default:
			// TODO error
		}
	}

	public void word(List<Arg> args) {
		checkOneArg(args);

		int value = getValue(args.get(0));
		// TODO handle error
		current += 2;
		objects.add(new WordItem(new Word(value)));
	}

	public void op(String command, List<Arg> args) {
		checkTwoArgs(args);

		switch (args.get(1)) {
		case Arg.Symbol(var mode):
			UByte opcode = opTable.lookup(command, mode);
			
			current += 1;
			objects.add(new ByteItem(opcode));
			
			var op = opTable.get(opcode);
			if (op.hasOperand()) {
				current += 2;
				if (args.get(0) instanceof Arg.Symbol(var name)) {
					objects.add(new SymbolItem(name));
				} else {
					int value = getValue(args.get(0));
					// TODO handle error
					objects.add(new WordItem(new Word(value)));
				}
			}
			break;
		default:
			// TODO error
		}
	}

	private void checkOneArg(List<Arg> args) {
		if (args.size() != 1) {
			// TODO error
		}
	}

	private void checkTwoArgs(List<Arg> args) {
		if (args.size() != 2) {
			// TODO error
		}
	}
}
