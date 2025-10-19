package edu.depauw.dep10;

public interface Arg {
	static Arg fromString(String s) {
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
		} else return null;
	}

	static record Symbol(String name) implements Arg {
	}

	static record Number(int value) implements Arg {
	}
	
	static record StrLit(String value) implements Arg {
	}
	
	static record CharLit(Character value) implements Arg {
	}
}
