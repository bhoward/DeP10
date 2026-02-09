package edu.depauw.dep10.assemble;

import edu.depauw.dep10.Word;

public interface Value {
	static Value fromString(String s) {
		if (Character.isAlphabetic(s.charAt(0)) || s.startsWith("_")) {
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
		
		public Word evaluate(Section section) throws ValueError {
		    var value = section.lookup(name);
		    if (value != null) {
		        return value.evaluate(section);
		    } else {
		        throw new ValueError("Symbol not found: " + name);
		    }
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
		
		public Word evaluate(Section section) {
		    return new Word(value);
		}
		
		@Override
		public String toString() {
			return Integer.toString(value);
		}
	}

	static record RelativeNumber(Section section, int value) implements Value {
		public int size() {
			return 2;
		}
		
		public Word evaluate(Section s) {
		    return new Word(section.getOrigin() + value);
		}
	}

	static record StrLit(String value) implements Value {
		public int size() {
			return value.length();
		}
		
		public Word evaluate(Section section) throws ValueError {
		    if (value.length() == 1) {
		        return new Word(value.charAt(0));
		    } else if (value.length() == 2) {
		        return new Word(value.charAt(0) * 256 + value.charAt(1));
		    } else {
		        throw new ValueError("String needs to be one or two characters: \"" + value + "\"");
		    }
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
		
		public Word evaluate(Section section) {
		    return new Word(value);
		}
		
		@Override
		public String toString() {
			return "'" + Util.escape(value) + "'";
		}
	}

	static record Block(int size) implements Value {
        public Word evaluate(Section section) throws ValueError {
            throw new ValueError("Illegal value");
        }
	}

	static record LowByte(Value arg) implements Value {
		public int size() {
			return 1;
		}

        public Word evaluate(Section section) throws ValueError {
            throw new ValueError("Illegal value");
        }
	}

	public int size();
	public Word evaluate(Section section) throws ValueError;
}
