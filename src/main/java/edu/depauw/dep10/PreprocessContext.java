package edu.depauw.dep10;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class PreprocessContext implements Iterable<Line> {
	private Stack<Iterator<Line>> stack;

	public PreprocessContext() {
		stack = new Stack<>();
	}

	public Iterator<Line> iterator() {
		return new Iterator<>() {
			public boolean hasNext() {
				while (!stack.isEmpty() && !stack.peek().hasNext()) {
					stack.pop();
				}

				return !stack.isEmpty();
			}

			public Line next() {
				// Precondition: hasNext() is true
				return stack.peek().next();
			}
		};
	}

	// Note that these all affect the underlying iterators that supply
	// the result of iterator()!
	public void pushReader(Reader in) {
		stack.push(Parser.parse(in).iterator());
	}

	public void pushLines(List<Line> lines) {
		stack.push(lines.iterator());
	}

	public List<Line> extractUntil(String end) {
		List<Line> result = new ArrayList<>();

		var it = stack.peek();
		while (it.hasNext()) {
			var line = it.next();
			if (line.command().equalsIgnoreCase(end)) {
				break;
			} else {
				result.add(line);
			}
		}

		return result;
	}
}
