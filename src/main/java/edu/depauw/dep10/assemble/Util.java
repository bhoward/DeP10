package edu.depauw.dep10.assemble;

public class Util {
	public static String processEscape(String s) {
		var b = new StringBuilder();

		LOOP: for (int i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '\\') {
				i++;
				if (i == s.length())
					break LOOP;
				switch (s.charAt(i)) {
				case 'b':
					b.append('\b');
					break;
				case 'f':
					b.append('\f');
					break;
				case 'n':
					b.append('\n');
					break;
				case 'r':
					b.append('\r');
					break;
				case 't':
					b.append('\t');
					break;
				case 'v':
					b.append('\013');
					break;
				case '"':
					b.append('"');
					break;
				case '\'':
					b.append('\'');
					break;
				case '\\':
					b.append('\\');
					break;
				case '0':
					b.append('\0');
					break;
				case 'X':
				case 'x':
					i += 2;
					if (i >= s.length())
						break LOOP;
					var h = s.substring(i - 1, i + 1);
					b.append((char) Integer.parseInt(h, 16));
					break;
				default:
					break LOOP; // stop on seeing an invalid character
				}
			} else {
				b.append(c);
			}
		}

		return b.toString();
	}

	public static String escape(char c) {
		switch (c) {
		case '\b': return "\\b";
		case '\f': return "\\f";
		case '\n': return "\\n";
		case '\r': return "\\r";
		case '\t': return "\\t";
		case '\013': return "\\v";
		case '"': return "\"";
		case '\'': return "'";
		case '\\': return "\\\\";
		case '\0': return "\\0";
		default:
			if (32 <= c && c <= 126) {
				return String.valueOf(c);
			} else {
				var hex = Integer.toHexString(c);
				if (c < 16) hex = "0" + hex;
				return "\\x" + hex;
			}
		}
	}
}
