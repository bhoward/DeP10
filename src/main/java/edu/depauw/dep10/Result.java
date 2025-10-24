package edu.depauw.dep10;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class Result {
	private int origin;
	private List<UByte> data;

	public Result(int origin) {
		this.origin = origin;
		this.data = new ArrayList<>();
	}

	public void add(UByte b) {
		data.add(b);
	}
	
	public String toString() {
		var builder = new StringBuilder();
		var format = HexFormat.of().withUpperCase();
		
		if (origin != 0) {
			builder.append("[" + format.toHexDigits(origin, 4) + "]\n");
		}
		
		var col = 0;
		for (var b : data) {
			format.toHexDigits(builder, (byte) b.value());
			col++;
			if (col == 8) {
				builder.append('\n');
				col = 0;
			} else {
				builder.append(' ');
			}
		}
		
		return builder.toString();
	}
}
