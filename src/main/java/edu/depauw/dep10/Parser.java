package edu.depauw.dep10;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Pattern;

public class Parser {
	static final String SYMBOL_PAT = "\\p{Alpha}\\w*";
	static final String COMMAND_PAT = "[.@]?\\p{Alpha}+";
	static final String INT_LIT_PAT = "-?[1-9]\\p{Digit}*|0";
	static final String HEX_LIT_PAT = "0[Xx][0-9A-Fa-f]{1,4}";
	static final String ESC_CHAR_PAT = "\\\\[\\\\bfnrtv0\"']";
	static final String HEX_CHAR_PAT = "\\\\[Xx][0-9A-Fa-f]{2}";
	static final String STR_LIT_PAT = "\"([^\"\\\\]|" + ESC_CHAR_PAT + "|" + HEX_CHAR_PAT + ")*\"";
	static final String CHAR_LIT_PAT = "'([^'\\\\]|" + ESC_CHAR_PAT + "|" + HEX_CHAR_PAT + ")'";

	static final String ARG_PAT = "(" + SYMBOL_PAT
			+ "|" + INT_LIT_PAT
			+ "|" + HEX_LIT_PAT
			+ "|" + STR_LIT_PAT
			+ "|" + CHAR_LIT_PAT + ")";

	static final String LINE_PAT = "^\\s*(?<label>(" + SYMBOL_PAT + ":)?)"
			+ "\\s*(?<command>(" + COMMAND_PAT + ")?)"
			+ "\\s*(?<args>(" + ARG_PAT + "(\\s*,\\s*" + ARG_PAT + ")*)?)"
			+ "\\s*(?<comment>(;.*)?)$";

	static final Pattern LINE = Pattern.compile(LINE_PAT);
	static final Pattern ARG = Pattern.compile("\\s*" + ARG_PAT + "\\s*,"); // Include a trailing comma

	// Note that this eagerly reads and then closes the Reader
	public static Iterable<Line> parse(Reader in) {
		var lines = new BufferedReader(in).lines();
		var result = lines.map(s -> parseLine(s)).filter(l -> !l.isEmpty()).toList();
		
		try {
			in.close();
		} catch (IOException e) {
			// We don't care about IOException on close of a Reader.
		}
		
		return result;
	}

	private static Line parseLine(String s) {
		var m = LINE.matcher(s);
		if (m.matches()) {
			String label = m.group("label");
			String command = m.group("command");
			String args = m.group("args");
			String comment = m.group("comment");
			return new Line(label, command, parseArgs(args), comment);
		} else {
			return new Line("", "", null, "");
		}
	}

	private static List<Value> parseArgs(String s) {
		var s2 = s + ",";
		var m = ARG.matcher(s2);
		return m.results().map(result -> Value.fromString(s2.substring(result.start(), result.end() - 1).trim())).toList();
	}

	public static void main(String[] args) {
		String test = """
				foo:.word 1
				 bar: .byte 2
					baz:	.ascii "hello\\b\\f\\n\\r\\t\\v\\"\\'\\\\\\x2A\\0"
					.block 42 ; that's the answer!
				@demo this, is ,'a',"test of args"	,	42, -17, x37, 0xDEAD , '\\'', '\\X42'
				""";
		System.out.println(test);

		for (var line : parse(new StringReader(test))) {
			System.out.println(line);
		}
	}
}
