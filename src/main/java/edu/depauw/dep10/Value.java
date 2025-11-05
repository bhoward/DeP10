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

	static record Symbol(String name) implements Value {
		public int size() {
			return 2;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	static record Number(int value) implements Value {
		public int size() {
			return 2;
		}
		
		@Override
		public String toString() {
			return Integer.toString(value);
		}
	}

	static record RelativeNumber(int value) implements Value {
		public int size() {
			return 2;
		}
	}

	static record StrLit(String value) implements Value {
		public int size() {
			return value.length();
		}
		
		@Override
		public String toString() {
			var builder = new StringBuilder();
			
			builder.append('"');
			for (int i = 0; i < value.length(); i++) {
				builder.append(Util.escape(value.charAt(i)));
			}
			builder.append('"');
			
			return builder.toString();
		}
	}

	static record CharLit(Character value) implements Value {
		public int size() {
			return 1;
		}
		
		@Override
		public String toString() {
			return "'" + Util.escape(value) + "'";
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
