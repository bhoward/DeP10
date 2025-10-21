package edu.depauw.dep10;

public interface Value {
	static Value fromString(String s) {
		if (Character.isAlphabetic(s.charAt(0))) {
			return new Symbol(s);
		} else if (s.startsWith("0x")) {
			return new Number(Integer.parseInt(s.substring(2), 16));
		} else if (Character.isDigit(s.charAt(0)) || s.startsWith("-")) {
			return new Number(Integer.parseInt(s));
		} else if (s.startsWith("\"")) {
			var e = Util.processEscape(s.substring(1, s.length() - 1));
			return new StrLit(e);
		} else if (s.startsWith("'")) {
			var e = Util.processEscape(s.substring(1, s.length() - 1));
			return new CharLit(e.charAt(0));
		} else
			return null;
	}

	default Value asWord() {
		return this;
	}

	static record Symbol(String name) implements Value {
		public int size() {
			return 2;
		}
	}

	static record Number(int value) implements Value {
		public int size() {
			return 2;
		}
	}

	static record StrLit(String value) implements Value {
		public int size() {
			return value.length();
		}
		
		@Override
		public Value asWord() {
			if (value.length() == 1) {
				return new Number(value.charAt(0));
			} else if (value.length() == 2) {
				return new Number(value.charAt(0) << 8 + value.charAt(1));
			} else {
				return null; // TODO error
			}
		}
	}

	static record CharLit(Character value) implements Value {
		public int size() {
			return 1;
		}
		
		@Override
		public Value asWord() {
			return new Number(value.charValue());
		}
	}

	static record Block(int size) implements Value {
	}

	static record LowByte(Value arg) implements Value {
		public int size() {
			return 1;
		}
	}

	public int size();
}
